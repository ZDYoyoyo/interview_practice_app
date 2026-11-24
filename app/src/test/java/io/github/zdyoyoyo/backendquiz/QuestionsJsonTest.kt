package io.github.zdyoyoyo.backendquiz

import io.github.zdyoyoyo.backendquiz.data.remote.BankParser
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

    // 題庫不可為空（空的題庫會讓所有題目在 App 裡被標成移除）
    @Test
    fun `bank is not empty`() {
        assertTrue(BankParser.parse(file.readText()).bank.questions.isNotEmpty(), "questions 不可為空")
    }
}
