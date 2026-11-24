package io.github.zdyoyoyo.backendquiz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import io.github.zdyoyoyo.backendquiz.data.local.entity.AttemptEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.BankMetaEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.DomainCountRow
import io.github.zdyoyoyo.backendquiz.data.local.entity.MistakeEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.MistakeListRow
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionEntity
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionAttemptRow
import io.github.zdyoyoyo.backendquiz.data.local.entity.QuestionRefRow
import io.github.zdyoyoyo.backendquiz.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.flow.Flow

/*
 * DAO ≈ MyBatis Mapper：只寫介面與 SQL，實作由 Room 在編譯期產生（KSP）。
 * SQL 寫錯（欄位不存在等）會直接編譯失敗。
 * - suspend fun：執行一次就回傳
 * - 回傳 Flow：資料表一變動就自動推送新結果（畫面用這個會自動更新）
 */

@Dao
interface QuestionDao {
    @Query("SELECT id, contentHash, active FROM questions")
    suspend fun getRefs(): List<QuestionRefRow>

    @Upsert
    suspend fun upsertAll(items: List<QuestionEntity>)

    @Query("UPDATE questions SET active = 0 WHERE id IN (:ids)")
    suspend fun deactivate(ids: List<String>)

    @Query("SELECT * FROM questions WHERE active = 1")
    suspend fun getActive(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE active = 1 AND domainCode IN (:codes)")
    suspend fun getActiveByDomains(codes: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE active = 1 AND id IN (:ids)")
    suspend fun getActiveByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Query("SELECT domainCode, COUNT(*) AS count FROM questions WHERE active = 1 GROUP BY domainCode")
    fun observeDomainCounts(): Flow<List<DomainCountRow>>

    @Query("SELECT COUNT(*) FROM questions WHERE active = 1")
    fun observeActiveCount(): Flow<Int>
}

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts WHERE questionId = :questionId ORDER BY answeredAt")
    suspend fun forQuestion(questionId: String): List<AttemptEntity>

    /** 每題的作答次數與答對次數。包含已被移除的題目（歷史紀錄仍算進統計） */
    @Query(
        """
        SELECT a.questionId AS questionId, q.domainCode AS domainCode,
               COUNT(*) AS total, SUM(a.correct) AS correct
        FROM attempts a JOIN questions q ON q.id = a.questionId
        GROUP BY a.questionId
        """
    )
    suspend fun questionStats(): List<QuestionAttemptRow>

    @Query("SELECT COUNT(*) FROM attempts WHERE answeredAt >= :fromMillis")
    suspend fun countSince(fromMillis: Long): Int

    @Query(
        """
        SELECT a.questionId AS questionId, q.domainCode AS domainCode,
               COUNT(*) AS total, SUM(a.correct) AS correct
        FROM attempts a JOIN questions q ON q.id = a.questionId
        GROUP BY a.questionId
        """
    )
    fun observeQuestionStats(): Flow<List<QuestionAttemptRow>>
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_states WHERE questionId = :questionId")
    suspend fun get(questionId: String): ReviewStateEntity?

    @Upsert
    suspend fun upsert(state: ReviewStateEntity)

    /** 今天（含過期）該複習、且仍在題庫中的題數 */
    @Query(
        """
        SELECT COUNT(*) FROM review_states r JOIN questions q ON q.id = r.questionId
        WHERE q.active = 1 AND r.dueEpochDay <= :today
        """
    )
    fun observeDueCount(today: Long): Flow<Int>

    /** 該複習的題目，過期最久的排前面 */
    @Query(
        """
        SELECT r.questionId FROM review_states r JOIN questions q ON q.id = r.questionId
        WHERE q.active = 1 AND r.dueEpochDay <= :today
        ORDER BY r.dueEpochDay, r.questionId
        """
    )
    suspend fun dueIds(today: Long): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM review_states r JOIN questions q ON q.id = r.questionId
        WHERE q.active = 1 AND r.dueEpochDay <= :today
        """
    )
    suspend fun dueCount(today: Long): Int
}

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes WHERE questionId = :questionId")
    suspend fun get(questionId: String): MistakeEntity?

    @Upsert
    suspend fun upsert(mistake: MistakeEntity)

    @Query("UPDATE mistakes SET mastered = 1 WHERE questionId = :questionId")
    suspend fun markMastered(questionId: String)

    /** 錯題本清單：未過關，且題目仍在題庫中（已被移除的題目不顯示，但紀錄保留） */
    @Query(
        """
        SELECT m.*, q.domainCode, q.stem FROM mistakes m
        JOIN questions q ON q.id = m.questionId
        WHERE m.mastered = 0 AND q.active = 1
        ORDER BY m.lastWrongAt DESC
        """
    )
    fun observeOpen(): Flow<List<MistakeListRow>>

    @Query(
        """
        SELECT m.questionId FROM mistakes m
        JOIN questions q ON q.id = m.questionId
        WHERE m.mastered = 0 AND q.active = 1
        ORDER BY m.lastWrongAt DESC
        """
    )
    suspend fun openIds(): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM mistakes m JOIN questions q ON q.id = m.questionId
        WHERE m.mastered = 0 AND q.active = 1
        """
    )
    suspend fun openCount(): Int
}

@Dao
interface BankMetaDao {
    @Query("SELECT * FROM bank_meta WHERE id = 0")
    suspend fun get(): BankMetaEntity?

    @Query("SELECT * FROM bank_meta WHERE id = 0")
    fun observe(): Flow<BankMetaEntity?>

    @Upsert
    suspend fun upsert(meta: BankMetaEntity)
}
