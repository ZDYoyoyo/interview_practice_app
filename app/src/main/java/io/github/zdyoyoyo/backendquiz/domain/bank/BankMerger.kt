package io.github.zdyoyoyo.backendquiz.domain.bank

import io.github.zdyoyoyo.backendquiz.domain.model.Question

/** 本機資料庫裡已存在的題目（只需要比對用的欄位） */
data class LocalQuestionRef(val id: String, val contentHash: String, val active: Boolean)

data class HashedQuestion(val question: Question, val contentHash: String)

/**
 * 合併計畫：要寫進資料庫的題目，以及統計數字。
 * 注意：作答紀錄、錯題、排程「完全不在這裡處理」——它們只用 questionId 對應，
 * 題目更新時 id 不變，所以紀錄自然還對得上；題目被移除時只是 active=false，紀錄也不會被刪。
 */
data class MergePlan(
    val upserts: List<HashedQuestion>,
    val addedIds: List<String>,
    val updatedIds: List<String>,
    /** 曾被移除、這次又出現在題庫裡的題目 */
    val reactivatedIds: List<String>,
    /** 這次題庫裡沒有了 → 標記為 inactive，不再出題 */
    val retiredIds: List<String>,
) {
    val hasChanges: Boolean
        get() = upserts.isNotEmpty() || retiredIds.isNotEmpty()
}

object BankMerger {

    fun plan(local: List<LocalQuestionRef>, incoming: List<Question>): MergePlan {
        val localById = local.associateBy { it.id } // ≈ stream().collect(toMap(...))
        val incomingIds = incoming.map { it.id }.toSet()

        val upserts = mutableListOf<HashedQuestion>()
        val added = mutableListOf<String>()
        val updated = mutableListOf<String>()
        val reactivated = mutableListOf<String>()

        for (q in incoming) {
            val hash = ContentHasher.hash(q)
            val existing = localById[q.id]
            when {
                existing == null -> { added += q.id; upserts += HashedQuestion(q, hash) }
                !existing.active -> { reactivated += q.id; upserts += HashedQuestion(q, hash) }
                existing.contentHash != hash -> { updated += q.id; upserts += HashedQuestion(q, hash) }
                else -> Unit // 內容沒變，不用寫入
            }
        }

        val retired = local.filter { it.active && it.id !in incomingIds }.map { it.id }
        return MergePlan(upserts, added, updated, reactivated, retired)
    }
}
