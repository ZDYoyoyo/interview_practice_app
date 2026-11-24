package io.github.zdyoyoyo.backendquiz.data.local

import io.github.zdyoyoyo.backendquiz.data.local.entity.MistakeEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionEntity
import io.github.zdyoyoyo.backendquiz.data.remote.OptionDto
import io.github.zdyoyoyo.backendquiz.domain.bank.HashedQuestion
import io.github.zdyoyoyo.backendquiz.domain.mistakes.MistakeRecord
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionOption
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Entity（資料庫形狀）與 domain model（程式邏輯用的形狀）互轉。
// 用擴充函式寫，呼叫時就像 entity.toDomain()。

private val json = Json

fun HashedQuestion.toEntity(version: Int): QuestionEntity = QuestionEntity(
    id = question.id,
    domainCode = question.domainCode,
    sourceTitle = question.sourceTitle,
    type = question.type.code,
    stem = question.stem,
    optionsJson = json.encodeToString(question.options.map { OptionDto(it.key, it.text) }),
    answerJson = json.encodeToString(question.answer.sorted()),
    explanation = question.explanation,
    difficulty = question.difficulty,
    contentHash = contentHash,
    active = true,
    updatedInVersion = version,
)

fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    domainCode = domainCode,
    sourceTitle = sourceTitle,
    type = QuestionType.fromCode(type) ?: QuestionType.SINGLE,
    stem = stem,
    options = json.decodeFromString<List<OptionDto>>(optionsJson).map { QuestionOption(it.key, it.text) },
    answer = json.decodeFromString<List<String>>(answerJson).toSet(),
    explanation = explanation,
    difficulty = difficulty,
)

fun MistakeEntity.toDomain() = MistakeRecord(questionId, wrongCount, correctStreak, mastered, firstWrongAt, lastWrongAt)

fun MistakeRecord.toEntity() = MistakeEntity(questionId, wrongCount, correctStreak, mastered, firstWrongAt, lastWrongAt)
