package io.github.zdyoyoyo.backendquiz

import io.github.zdyoyoyo.backendquiz.data.remote.BankParser
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 驗證 repo 裡真正的 data/questions.json。
 * 外部流程每次更新題庫，CI 都會跑這個測試；任何 ERROR 或 WARNING 都會讓 CI 失敗。
 */
class QuestionsJsonTest {

    private val file = File(System.getProperty("bankFile") ?: "../data/questions.json")

    @Test
    // 題庫格式完全正確
    fun `bank file is fully valid`() {
        val text = file.readText()
        val parsed = BankParser.parse(text)
        if (parsed.issues.isNotEmpty()) {
            fail("data/questions.json 有 ${parsed.issues.size} 個問題：\n" + parsed.issues.joinToString("\n"))
        }
        assertTrue(parsed.bank.version >= 1, "version 必須 >= 1")
        assertTrue(parsed.bank.updatedAt.isNotBlank(), "updatedAt 不可為空")
    }

    @Test
    // 範例題庫涵蓋單選、多選與程式碼題
    fun `bank covers single multi and code questions`() {
        val questions = BankParser.parse(file.readText()).bank.questions
        assertTrue(questions.any { it.type == QuestionType.SINGLE })
        assertTrue(questions.any { it.type == QuestionType.MULTI })
        assertTrue(questions.any { "```" in it.stem })
    }
}
