package io.github.zdyoyoyo.backendquiz.data.remote

import io.github.zdyoyoyo.backendquiz.domain.bank.BankIssue
import io.github.zdyoyoyo.backendquiz.domain.bank.QuestionValidator
import io.github.zdyoyoyo.backendquiz.domain.bank.Severity
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionBank
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionOption
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** JSON 的形狀（DTO）。@Serializable 會在編譯期產生解析程式碼，不用反射。 */
@Serializable
data class QuestionDto(
    val id: String,
    val domain: String,
    val sourceTitle: String = "",
    val type: String,
    val stem: String,
    val options: List<OptionDto>,
    val answer: List<String>,
    val explanation: String = "",
    val difficulty: Int = 2,
)

@Serializable
data class OptionDto(val key: String, val text: String)

class BankFormatException(message: String) : Exception(message)

data class ParsedBank(
    /** 只包含通過驗證的題目 */
    val bank: QuestionBank,
    val issues: List<BankIssue>,
)

/**
 * 解析 questions.json。
 * 整份檔案壞掉（不是 JSON、沒有 version）→ 丟 [BankFormatException]；
 * 單一題目壞掉 → 記錄成 issue 並跳過，其他題照常使用。
 */
object BankParser {

    private val json = Json { ignoreUnknownKeys = true } // 題庫多了新欄位也不會壞

    fun parse(text: String): ParsedBank {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw BankFormatException("題庫不是合法的 JSON 物件：${e.message}")
        }
        val version = root["version"]?.jsonPrimitive?.intOrNull
            ?: throw BankFormatException("缺少整數欄位 version")
        val updatedAt = root["updatedAt"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val array = runCatching { root["questions"]?.jsonArray }.getOrNull()
            ?: throw BankFormatException("缺少 questions 陣列")

        val issues = mutableListOf<BankIssue>()
        val questions = mutableListOf<Question>()
        array.forEachIndexed { index, element ->
            val dto = try {
                json.decodeFromJsonElement<QuestionDto>(element)
            } catch (e: Exception) {
                issues += BankIssue(null, Severity.ERROR, "第 $index 題無法解析：${e.message?.lineSequence()?.first()}")
                return@forEachIndexed // ≈ Java forEach 裡的 continue
            }
            val type = QuestionType.fromCode(dto.type)
            if (type == null) {
                issues += BankIssue(dto.id, Severity.ERROR, "type 必須是 single 或 multi，實際為「${dto.type}」")
                return@forEachIndexed
            }
            questions += Question(
                id = dto.id,
                domainCode = dto.domain,
                sourceTitle = dto.sourceTitle,
                type = type,
                stem = dto.stem,
                options = dto.options.map { QuestionOption(it.key, it.text) },
                answer = dto.answer.toSet(),
                explanation = dto.explanation,
                difficulty = dto.difficulty,
            )
        }

        val validation = QuestionValidator.validate(questions)
        return ParsedBank(
            bank = QuestionBank(version, updatedAt, validation.valid),
            issues = issues + validation.issues,
        )
    }
}
