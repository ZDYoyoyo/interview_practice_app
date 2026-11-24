package io.github.zdyoyoyo.backendquiz.domain.quiz

import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerCheckerTest {
    private val single = question(type = QuestionType.SINGLE, answer = setOf("B"))
    private val multi = question(type = QuestionType.MULTI, answer = setOf("A", "C"))

    @Test
    // 單選題
    fun `single choice`() {
        assertTrue(AnswerChecker.isCorrect(single, setOf("B")))
        assertFalse(AnswerChecker.isCorrect(single, setOf("A")))
    }

    @Test
    // 多選題全對才算對
    fun `multi choice requires exact match`() {
        assertTrue(AnswerChecker.isCorrect(multi, setOf("C", "A")))
        assertFalse(AnswerChecker.isCorrect(multi, setOf("A")), "少選")
        assertFalse(AnswerChecker.isCorrect(multi, setOf("A", "B", "C")), "多選")
    }

    @Test
    // 沒選不算對
    fun `empty selection is wrong`() {
        assertFalse(AnswerChecker.isCorrect(single, emptySet()))
    }
}
