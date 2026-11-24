package io.github.zdyoyoyo.backendquiz.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本機題庫快取。題目被題庫移除時不刪除，只把 active 設為 false。
 * options / answer 以 JSON 字串存放（SQLite 沒有陣列型別）。
 */
@Entity(tableName = "questions", indices = [Index("domainCode"), Index("active")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val domainCode: String,
    val sourceTitle: String,
    val type: String,
    val stem: String,
    val optionsJson: String,
    val answerJson: String,
    val explanation: String,
    val difficulty: Int,
    val contentHash: String,
    val active: Boolean,
    /** 這題最後一次被新增或更新時的題庫版本 */
    val updatedInVersion: Int,
)

/**
 * 每一次作答。刻意「不」對 questions 設外鍵，
 * 題目被移除或更新時，作答紀錄完全不受影響，只靠 questionId 對應。
 */
@Entity(tableName = "attempts", indices = [Index("questionId"), Index("answeredAt")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    /** 選了哪些選項，例如 "A,C" */
    val selected: String,
    val correct: Boolean,
    val answeredAt: Long,
    val mode: String,
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val questionId: String,
    val wrongCount: Int,
    val correctStreak: Int,
    val mastered: Boolean,
    val firstWrongAt: Long,
    val lastWrongAt: Long,
)

/** 單列表格，存題庫目前的版本與下載資訊。和題目寫在同一個資料庫交易裡，確保一致。 */
@Entity(tableName = "bank_meta")
data class BankMetaEntity(
    @PrimaryKey val id: Int = 0,
    val version: Int,
    val updatedAt: String,
    val etag: String?,
    val lastCheckedAt: Long,
)

/** 查詢結果用的投影（不是資料表） */
data class QuestionRefRow(val id: String, val contentHash: String, val active: Boolean)

data class DomainCountRow(val domainCode: String, val count: Int)

data class MistakeListRow(
    @Embedded val mistake: MistakeEntity,
    val domainCode: String,
    val stem: String,
)
