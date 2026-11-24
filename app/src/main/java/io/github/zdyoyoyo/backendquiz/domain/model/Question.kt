package io.github.zdyoyoyo.backendquiz.domain.model

enum class QuestionType(val code: String) {
    SINGLE("single"),
    MULTI("multi");

    companion object {
        fun fromCode(code: String): QuestionType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * data class ≈ Java 的 record（或 Lombok @Value）：
 * 自動產生 equals / hashCode / toString / copy()，且屬性用 val 宣告就是不可變。
 */
data class QuestionOption(val key: String, val text: String)

data class Question(
    val id: String,
    /** 保留原始代碼字串（而不是直接轉成 enum），未知代碼也不會遺失。 */
    val domainCode: String,
    val sourceTitle: String,
    val type: QuestionType,
    val stem: String,
    val options: List<QuestionOption>,
    /** 正確選項的 key；單選題只有 1 個。用 Set 是因為多選題「全對才算對」就是集合相等。 */
    val answer: Set<String>,
    val explanation: String,
    val difficulty: Int,
) {
    // 自訂 getter：每次讀取時計算，不佔欄位
    val domain: Domain get() = Domain.fromCode(domainCode)
}

data class QuestionBank(
    val version: Int,
    val updatedAt: String,
    val questions: List<Question>,
)
