package io.github.zdyoyoyo.backendquiz.data.repository

import io.github.zdyoyoyo.backendquiz.TestQuestions.bankJson
import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
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
}
