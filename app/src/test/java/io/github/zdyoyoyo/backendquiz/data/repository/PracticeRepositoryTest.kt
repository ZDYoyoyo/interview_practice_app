package io.github.zdyoyoyo.backendquiz.data.repository

import io.github.zdyoyoyo.backendquiz.TestQuestions.bankJson
import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.data.local.toDomain
import io.github.zdyoyoyo.backendquiz.data.remote.FetchResult
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PracticeRepositoryTest : RepositoryTestBase() {

    private val single = question("s1", domain = "spring", answer = setOf("A"))
    private val multi = question("m1", domain = "database", type = QuestionType.MULTI, answer = setOf("A", "C"))

    private suspend fun seed() {
        bundled = bankJson(1, single, multi)
        sync.ensureSeeded()
    }

    @Test
    // 答錯自動收錄到錯題本，答對不收錄
    fun `wrong answers go to mistake book`() = runTest {
        seed()
        val wrong = practice.recordAnswer(multi, setOf("A"), QuizMode.RANDOM) // 少選也算錯
        assertFalse(wrong.correct)
        assertEquals(1, wrong.mistake!!.wrongCount)

        val right = practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM)
        assertTrue(right.correct)
        assertEquals(null, right.mistake)

        assertEquals(listOf("m1"), practice.observeOpenMistakes().first().map { it.record.questionId })
    }

    @Test
    // 錯題重做連對三次後移出錯題本
    fun `three correct redos master the mistake`() = runTest {
        seed()
        practice.recordAnswer(multi, setOf("B"), QuizMode.RANDOM)
        repeat(2) {
            val o = practice.recordAnswer(multi, setOf("A", "C"), QuizMode.MISTAKES)
            assertFalse(o.justMastered)
        }
        val third = practice.recordAnswer(multi, setOf("A", "C"), QuizMode.MISTAKES)
        assertTrue(third.justMastered)
        assertTrue(practice.observeOpenMistakes().first().isEmpty())
        // 歷史作答紀錄全部保留
        assertEquals(4, db.attemptDao().forQuestion("m1").size)
    }

    @Test
    // 錯題模式只出錯題本裡的題目
    fun `mistake mode only picks open mistakes`() = runTest {
        seed()
        practice.recordAnswer(single, setOf("B"), QuizMode.RANDOM)
        val questions = practice.questionsForSession(QuizMode.MISTAKES, emptySet(), 10)
        assertEquals(listOf("s1"), questions.map { it.id })
    }

    @Test
    // 依領域篩選出題
    fun `filters by domain`() = runTest {
        seed()
        val questions = practice.questionsForSession(QuizMode.RANDOM, setOf("database"), 10)
        assertEquals(listOf("m1"), questions.map { it.id })
    }

    @Test
    // 手動移出錯題本
    fun `manual removal from mistake book`() = runTest {
        seed()
        practice.recordAnswer(single, setOf("B"), QuizMode.RANDOM)
        practice.removeFromMistakes("s1")
        assertTrue(practice.observeOpenMistakes().first().isEmpty())
    }

    // 作答後建立 SM-2 排程：答對明天複習、答錯也是明天（但 EF 下降）
    @Test
    fun `answering creates review schedule`() = runTest {
        seed()
        goToDay(100)
        val right = practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM)
        assertEquals(101, right.review.dueEpochDay)
        assertEquals(null, right.reviewBefore)

        val wrong = practice.recordAnswer(multi, setOf("A"), QuizMode.RANDOM)
        assertEquals(101, wrong.review.dueEpochDay)
        assertTrue(wrong.review.easiness < right.review.easiness)
    }

    // 「今天該複習」：只算到期的；複習模式只出到期的題目
    @Test
    fun `due count and review mode`() = runTest {
        seed()
        goToDay(100)
        practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM)
        practice.recordAnswer(multi, setOf("A", "C"), QuizMode.RANDOM)
        assertEquals(0, practice.observeDueCount(100).first())
        assertTrue(practice.questionsForSession(QuizMode.REVIEW, emptySet(), 10).isEmpty())

        goToDay(101)
        assertEquals(2, practice.observeDueCount(101).first())
        assertEquals(setOf("s1", "m1"), practice.questionsForSession(QuizMode.REVIEW, emptySet(), 10).map { it.id }.toSet())

        // 複習答對後就不再是今天到期
        practice.recordAnswer(single, setOf("A"), QuizMode.REVIEW)
        assertEquals(1, practice.observeDueCount(101).first())
    }

    // 答對後自評：從作答前的狀態重算
    @Test
    fun `self rating recalculates from state before answer`() = runTest {
        seed()
        goToDay(100)
        practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM) // day100 → due 101
        goToDay(101)
        val outcome = practice.recordAnswer(single, setOf("A"), QuizMode.REVIEW) // 第二次 → 6 天
        assertEquals(107, outcome.review.dueEpochDay)

        val easy = practice.rateCorrectAnswer("s1", outcome.reviewBefore, Sm2Scheduler.QUALITY_EASY)
        assertEquals(2.6, easy.easiness, 1e-9)
        val hard = practice.rateCorrectAnswer("s1", outcome.reviewBefore, Sm2Scheduler.QUALITY_HARD)
        assertEquals(2.36, hard.easiness, 1e-9)
        assertEquals(107, hard.dueEpochDay)
        assertEquals(hard, db.reviewDao().get("s1")!!.toDomain())
    }

    // 題庫移除的題目：排程保留，但不算進今天該複習
    @Test
    fun `retired questions are not due`() = runTest {
        seed()
        goToDay(100)
        practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM)
        practice.recordAnswer(multi, setOf("A", "C"), QuizMode.RANDOM)
        remote.next = FetchResult.Fetched(bankJson(2, single), etag = null) // m1 被移除
        sync.syncRemote()

        goToDay(101)
        assertEquals(1, practice.observeDueCount(101).first())
        assertTrue(db.reviewDao().get("m1") != null)
    }

    // 依弱點出題：只從選定領域、有效題目中抽
    @Test
    fun `weakness mode respects domain filter`() = runTest {
        seed()
        val picked = practice.questionsForSession(QuizMode.WEAKNESS, setOf("spring"), 10)
        assertEquals(listOf("s1"), picked.map { it.id })
    }

    // 統計：作答後自動更新
    @Test
    fun `stats reflect attempts`() = runTest {
        seed()
        practice.recordAnswer(single, setOf("A"), QuizMode.RANDOM)
        practice.recordAnswer(multi, setOf("B"), QuizMode.RANDOM)
        val stats = practice.observeStats().first()
        assertEquals(2, stats.totalAttempts)
        assertEquals(1, stats.totalCorrect)
        assertEquals(2, stats.domains.size)
    }
}
