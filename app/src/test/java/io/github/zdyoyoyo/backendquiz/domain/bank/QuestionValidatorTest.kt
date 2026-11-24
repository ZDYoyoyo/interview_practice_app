package io.github.zdyoyoyo.backendquiz.domain.bank

import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionValidatorTest {

    private fun errorsFor(q: io.github.zdyoyoyo.backendquiz.domain.model.Question) =
        QuestionValidator.validate(listOf(q)).issues.filter { it.severity == Severity.ERROR }

    @Test
    // 合法題目沒有任何問題
    fun `valid questions have no issues`() {
        val result = QuestionValidator.validate(listOf(question("a"), question("b", type = QuestionType.MULTI)))
        assertEquals(2, result.valid.size)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    // single 題答案必須剛好一個
    fun `single requires exactly one answer`() {
        assertEquals(1, errorsFor(question(type = QuestionType.SINGLE, answer = setOf("A", "B"))).size)
    }

    @Test
    // multi 題答案至少兩個
    fun `multi requires at least two answers`() {
        assertEquals(1, errorsFor(question(type = QuestionType.MULTI, answer = setOf("A"))).size)
    }

    @Test
    // 答案必須是存在的選項
    fun `answer must reference existing option`() {
        assertEquals(1, errorsFor(question(answer = setOf("Z"))).size)
    }

    @Test
    // id 格式限制
    fun `id format rules`() {
        listOf("Has-Upper", "has space", "under_score", "-leading", "a".repeat(65)).forEach {
            assertEquals(1, errorsFor(question(id = it)).size, "id=$it 應該不合法")
        }
    }

    @Test
    // difficulty 只能 1 到 3
    fun `difficulty must be 1 to 3`() {
        assertEquals(1, errorsFor(question(difficulty = 0)).size)
        assertEquals(1, errorsFor(question(difficulty = 4)).size)
    }

    @Test
    // 重複 id 只保留第一個
    fun `duplicate id keeps first only`() {
        val result = QuestionValidator.validate(listOf(question("dup", stem = "第一"), question("dup", stem = "第二")))
        assertEquals("第一", result.valid.single().stem)
        assertEquals(1, result.issues.size)
    }

    @Test
    // 未知領域只是警告，題目仍可使用
    fun `unknown domain is warning and question still usable`() {
        val result = QuestionValidator.validate(listOf(question(domain = "quantum")))
        assertEquals(1, result.valid.size)
        assertEquals(Severity.WARNING, result.issues.single().severity)
    }
}
