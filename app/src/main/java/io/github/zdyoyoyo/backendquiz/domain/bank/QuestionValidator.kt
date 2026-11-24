package io.github.zdyoyoyo.backendquiz.domain.bank

import io.github.zdyoyoyo.backendquiz.domain.model.Domain
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType

enum class Severity { ERROR, WARNING }

data class BankIssue(val questionId: String?, val severity: Severity, val message: String) {
    override fun toString(): String = "[$severity] ${questionId ?: "-"}: $message"
}

data class ValidationResult(
    /** 通過驗證、可以出題的題目 */
    val valid: List<Question>,
    val issues: List<BankIssue>,
)

/**
 * 題目內容規則（README 的欄位規則就是照這裡寫的）。
 *
 * 執行時的策略：
 * - App：ERROR 的題目跳過（不出題），其他照常使用 → 一題壞掉不會讓整個題庫失效
 * - CI 測試：只要有任何 issue（含 WARNING）就失敗 → 壞資料進不了 main
 */
object QuestionValidator {

    /** 小寫英數與連字號，1–64 字元，例如 jvm-gc-001 */
    val ID_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private const val MAX_ID_LENGTH = 64

    fun validate(questions: List<Question>): ValidationResult {
        val issues = mutableListOf<BankIssue>()
        val valid = mutableListOf<Question>()
        val seenIds = HashSet<String>()

        for (q in questions) {
            val errors = errorsOf(q)
            if (!seenIds.add(q.id)) {
                errors += "id 重複（只保留第一個）"
            }
            if (errors.isEmpty()) {
                valid += q
            } else {
                errors.forEach { issues += BankIssue(q.id, Severity.ERROR, it) }
            }
            if (q.domainCode !in Domain.knownCodes) {
                issues += BankIssue(q.id, Severity.WARNING, "未知的 domain「${q.domainCode}」，App 會歸類到「其他」")
            }
        }
        return ValidationResult(valid, issues)
    }

    private fun errorsOf(q: Question): MutableList<String> {
        val errors = mutableListOf<String>()
        if (q.id.length > MAX_ID_LENGTH || !ID_PATTERN.matches(q.id)) {
            errors += "id 格式錯誤，只能用小寫英數與連字號（最長 $MAX_ID_LENGTH）"
        }
        if (q.stem.isBlank()) errors += "stem 不可為空"
        if (q.options.size < 2) errors += "options 至少要 2 個"

        val keys = q.options.map { it.key }
        if (keys.toSet().size != keys.size) errors += "options 的 key 重複"
        if (q.options.any { it.key.isBlank() || it.text.isBlank() }) errors += "options 的 key 與 text 不可為空"

        val unknownAnswers = q.answer - keys.toSet()
        if (unknownAnswers.isNotEmpty()) errors += "answer 含有不存在的選項：$unknownAnswers"

        when (q.type) {
            QuestionType.SINGLE -> if (q.answer.size != 1) errors += "single 題的 answer 必須剛好 1 個"
            QuestionType.MULTI -> if (q.answer.size < 2) errors += "multi 題的 answer 至少要 2 個"
        }
        if (q.difficulty !in 1..3) errors += "difficulty 必須是 1、2 或 3"
        return errors
    }
}
