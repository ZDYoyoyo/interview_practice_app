package io.github.zdyoyoyo.backendquiz.data.repository

import androidx.room.withTransaction
import io.github.zdyoyoyo.backendquiz.data.local.AppDatabase
import io.github.zdyoyoyo.backendquiz.data.local.entity.AttemptEntity
import io.github.zdyoyoyo.backendquiz.data.local.toDomain
import io.github.zdyoyoyo.backendquiz.data.local.toEntity
import io.github.zdyoyoyo.backendquiz.domain.mistakes.MistakePolicy
import io.github.zdyoyoyo.backendquiz.domain.mistakes.MistakeRecord
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.quiz.AnswerChecker
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuestionPicker
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import io.github.zdyoyoyo.backendquiz.domain.srs.ReviewState
import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler
import io.github.zdyoyoyo.backendquiz.domain.stats.StatsCalculator
import io.github.zdyoyoyo.backendquiz.domain.stats.StatsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

data class AnswerOutcome(
    val correct: Boolean,
    /** 作答後的錯題紀錄；null = 不在錯題本 */
    val mistake: MistakeRecord?,
    /** 這次作答剛好過三關 */
    val justMastered: Boolean,
    /** 作答前的複習狀態（答對後自評時，要從這個狀態重新計算） */
    val reviewBefore: ReviewState?,
    /** 作答後的複習狀態 */
    val review: ReviewState,
)

data class MistakeItem(val record: MistakeRecord, val domainCode: String, val stem: String)

class PracticeRepository(
    private val db: AppDatabase,
    private val picker: QuestionPicker = QuestionPicker(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val questionDao = db.questionDao()
    private val attemptDao = db.attemptDao()
    private val mistakeDao = db.mistakeDao()
    private val reviewDao = db.reviewDao()

    /** 「今天」以手機的時區計算 */
    fun currentEpochDay(): Long = Instant.ofEpochMilli(clock()).atZone(zone()).toLocalDate().toEpochDay()

    /** 題庫裡各領域（只算有效題目）的題數 */
    fun observeDomainCounts(): Flow<Map<String, Int>> =
        questionDao.observeDomainCounts().map { rows -> rows.associate { it.domainCode to it.count } }

    fun observeActiveCount(): Flow<Int> = questionDao.observeActiveCount()

    fun observeDueCount(today: Long): Flow<Int> = reviewDao.observeDueCount(today)

    fun observeStats(): Flow<StatsSummary> =
        attemptDao.observeQuestionStats().map { rows -> StatsCalculator.summarize(rows.map { it.toDomain() }) }

    fun observeOpenMistakes(): Flow<List<MistakeItem>> =
        mistakeDao.observeOpen().map { rows ->
            rows.map { MistakeItem(it.mistake.toDomain(), it.domainCode, it.stem) }
        }

    /**
     * @param domainCodes 空集合 = 全部領域
     */
    suspend fun questionsForSession(mode: QuizMode, domainCodes: Set<String>, count: Int): List<Question> {
        fun Question.inScope() = domainCodes.isEmpty() || domainCode in domainCodes

        return when (mode) {
            QuizMode.RANDOM -> picker.pickRandom(activePool(domainCodes), count)
            QuizMode.WEAKNESS -> picker.pickByWeakness(
                pool = activePool(domainCodes),
                stats = attemptDao.questionStats().map { it.toDomain() },
                count = count,
            )
            // 依資料庫排好的順序（最近答錯／過期最久的在前）
            QuizMode.MISTAKES -> loadActiveInOrder(mistakeDao.openIds()).filter { it.inScope() }.take(count)
            QuizMode.REVIEW -> loadActiveInOrder(reviewDao.dueIds(currentEpochDay())).filter { it.inScope() }.take(count)
        }
    }

    private suspend fun activePool(domainCodes: Set<String>): List<Question> =
        (if (domainCodes.isEmpty()) questionDao.getActive() else questionDao.getActiveByDomains(domainCodes.toList()))
            .map { it.toDomain() }

    private suspend fun loadActiveInOrder(ids: List<String>): List<Question> {
        // SQLite 一次最多 999 個參數，分批查
        val byId = ids.chunked(500).flatMap { questionDao.getActiveByIds(it) }.associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    /** 寫入作答紀錄，並更新錯題本與 SM-2 排程（同一個交易） */
    suspend fun recordAnswer(question: Question, selected: Set<String>, mode: QuizMode): AnswerOutcome {
        val correct = AnswerChecker.isCorrect(question, selected)
        val now = clock()
        val today = currentEpochDay()
        return db.withTransaction {
            attemptDao.insert(
                AttemptEntity(
                    questionId = question.id,
                    selected = selected.sorted().joinToString(","),
                    correct = correct,
                    answeredAt = now,
                    mode = mode.code,
                )
            )

            val mistakeBefore = mistakeDao.get(question.id)?.toDomain()
            val mistakeAfter = MistakePolicy.onAnswer(mistakeBefore, question.id, correct, now)
            if (mistakeAfter != null) mistakeDao.upsert(mistakeAfter.toEntity())

            val reviewBefore = reviewDao.get(question.id)?.toDomain()
            val quality = if (correct) Sm2Scheduler.QUALITY_GOOD else Sm2Scheduler.QUALITY_WRONG
            val reviewAfter = Sm2Scheduler.review(reviewBefore, question.id, quality, today)
            reviewDao.upsert(reviewAfter.toEntity())

            AnswerOutcome(
                correct = correct,
                mistake = mistakeAfter ?: mistakeBefore?.takeIf { !it.mastered },
                justMastered = mistakeBefore?.mastered == false && mistakeAfter?.mastered == true,
                reviewBefore = reviewBefore,
                review = reviewAfter,
            )
        }
    }

    /**
     * 答對後的自評（秒答／想了一下／猜對）。
     * 從「作答前」的狀態用新的分數重算一次，取代作答時用預設分數 4 算出的結果。
     */
    suspend fun rateCorrectAnswer(questionId: String, reviewBefore: ReviewState?, quality: Int): ReviewState {
        require(quality >= Sm2Scheduler.PASS_QUALITY) { "自評只用在答對的題目" }
        val after = Sm2Scheduler.review(reviewBefore, questionId, quality, currentEpochDay())
        reviewDao.upsert(after.toEntity())
        return after
    }

    suspend fun removeFromMistakes(questionId: String) = mistakeDao.markMastered(questionId)
}
