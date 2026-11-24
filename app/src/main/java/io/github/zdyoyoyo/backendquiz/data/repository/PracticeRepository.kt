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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AnswerOutcome(
    val correct: Boolean,
    /** 作答後的錯題紀錄；null = 不在錯題本 */
    val mistake: MistakeRecord?,
    /** 這次作答剛好過三關 */
    val justMastered: Boolean,
)

data class MistakeItem(val record: MistakeRecord, val domainCode: String, val stem: String)

class PracticeRepository(
    private val db: AppDatabase,
    private val picker: QuestionPicker = QuestionPicker(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val questionDao = db.questionDao()
    private val attemptDao = db.attemptDao()
    private val mistakeDao = db.mistakeDao()

    /** 題庫裡各領域（只算有效題目）的題數 */
    fun observeDomainCounts(): Flow<Map<String, Int>> =
        questionDao.observeDomainCounts().map { rows -> rows.associate { it.domainCode to it.count } }

    fun observeActiveCount(): Flow<Int> = questionDao.observeActiveCount()

    fun observeOpenMistakes(): Flow<List<MistakeItem>> =
        mistakeDao.observeOpen().map { rows ->
            rows.map { MistakeItem(it.mistake.toDomain(), it.domainCode, it.stem) }
        }

    /**
     * @param domainCodes 空集合 = 全部領域
     */
    suspend fun questionsForSession(mode: QuizMode, domainCodes: Set<String>, count: Int): List<Question> =
        when (mode) {
            QuizMode.RANDOM -> {
                val pool = if (domainCodes.isEmpty()) questionDao.getActive()
                else questionDao.getActiveByDomains(domainCodes.toList())
                picker.pickRandom(pool.map { it.toDomain() }, count)
            }
            QuizMode.MISTAKES -> {
                val ids = mistakeDao.openIds()
                val byId = ids.chunked(500).flatMap { questionDao.getActiveByIds(it) }.associateBy { it.id }
                ids.mapNotNull { byId[it]?.toDomain() } // 保留「最近答錯的排前面」的順序
                    .filter { domainCodes.isEmpty() || it.domainCode in domainCodes }
                    .take(count)
            }
        }

    /** 寫入作答紀錄並更新錯題本（同一個交易） */
    suspend fun recordAnswer(question: Question, selected: Set<String>, mode: QuizMode): AnswerOutcome {
        val correct = AnswerChecker.isCorrect(question, selected)
        val now = clock()
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
            val before = mistakeDao.get(question.id)?.toDomain()
            val after = MistakePolicy.onAnswer(before, question.id, correct, now)
            if (after != null) mistakeDao.upsert(after.toEntity())

            AnswerOutcome(
                correct = correct,
                mistake = after ?: before?.takeIf { !it.mastered },
                justMastered = before?.mastered == false && after?.mastered == true,
            )
        }
    }

    suspend fun removeFromMistakes(questionId: String) = mistakeDao.markMastered(questionId)
}
