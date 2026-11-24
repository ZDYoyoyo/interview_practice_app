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

/**
 * SM-2 複習排程（第二階段新增，資料庫 v2）。
 * 和 attempts 一樣只用 questionId 對應題目、不設外鍵：題目更新後排程照舊，題目移除後排程保留但不再出題。
 */
@Entity(tableName = "review_states", indices = [Index("dueEpochDay")])
data class ReviewStateEntity(
    @PrimaryKey val questionId: String,
    val easiness: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueEpochDay: Long,
    val lastReviewEpochDay: Long?,
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

data class QuestionAttemptRow(
    val questionId: String,
    val domainCode: String,
    val total: Int,
    val correct: Int,
)
