package io.github.zdyoyoyo.backendquiz

import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionOption
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType

/** 測試用的題目工廠：只填需要的欄位，其他用預設值（Kotlin 具名參數 + 預設值，免寫 Builder） */
object TestQuestions {
    fun question(
        id: String = "q-1",
        domain: String = "java_basics",
        type: QuestionType = QuestionType.SINGLE,
        stem: String = "題幹 $id",
        options: List<QuestionOption> = listOf(QuestionOption("A", "a"), QuestionOption("B", "b"), QuestionOption("C", "c")),
        answer: Set<String> = if (type == QuestionType.SINGLE) setOf("A") else setOf("A", "B"),
        explanation: String = "解析",
        difficulty: Int = 2,
    ) = Question(id, domain, "來源 $id", type, stem, options, answer, explanation, difficulty)

    /** 產生 questions.json 字串 */
    fun bankJson(version: Int, vararg questions: Question): String = buildString {
        append("""{"version":$version,"updatedAt":"2026-01-01T00:00:00Z","questions":[""")
        questions.forEachIndexed { i, q ->
            if (i > 0) append(',')
            append("""{"id":"${q.id}","domain":"${q.domainCode}","sourceTitle":"${q.sourceTitle}","type":"${q.type.code}",""")
            append(""""stem":"${q.stem}","options":[""")
            append(q.options.joinToString(",") { """{"key":"${it.key}","text":"${it.text}"}""" })
            append("""],"answer":[${q.answer.joinToString(",") { "\"$it\"" }}],""")
            append(""""explanation":"${q.explanation}","difficulty":${q.difficulty}}""")
        }
        append("]}")
    }
}
