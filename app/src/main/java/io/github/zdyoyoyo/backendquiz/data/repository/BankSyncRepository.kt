package io.github.zdyoyoyo.backendquiz.data.repository

import androidx.room.withTransaction
import io.github.zdyoyoyo.backendquiz.data.local.AppDatabase
import io.github.zdyoyoyo.backendquiz.data.local.entity.BankMetaEntity
import io.github.zdyoyoyo.backendquiz.data.local.toEntity
import io.github.zdyoyoyo.backendquiz.data.remote.BankParser
import io.github.zdyoyoyo.backendquiz.data.remote.BankRemoteSource
import io.github.zdyoyoyo.backendquiz.data.remote.FetchResult
import io.github.zdyoyoyo.backendquiz.domain.bank.BankMerger
import io.github.zdyoyoyo.backendquiz.domain.bank.BankVersionPolicy
import io.github.zdyoyoyo.backendquiz.domain.bank.LocalQuestionRef
import io.github.zdyoyoyo.backendquiz.domain.bank.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** sealed interface ≈ Java 17 的 sealed interface：when 判斷時編譯器會檢查有沒有漏掉的情況 */
sealed interface SyncOutcome {
    data class Updated(
        val fromVersion: Int?,
        val toVersion: Int,
        val added: Int,
        val updated: Int,
        val retired: Int,
        /** 格式有問題、被跳過的題目數 */
        val skipped: Int,
    ) : SyncOutcome

    data class UpToDate(val version: Int) : SyncOutcome
    data class Failed(val reason: String) : SyncOutcome
}

/**
 * 題庫同步流程：
 *   1. [ensureSeeded]：用 App 內建的題庫（assets/questions.json）初始化，第一次開 App 沒網路也能用
 *   2. [syncRemote]：帶 ETag 向 GitHub 要最新版，版本較新才合併
 * 兩者都走 [applyIfNewer]，同一套「版本比對 → 計算合併計畫 → 單一交易寫入」。
 */
class BankSyncRepository(
    private val db: AppDatabase,
    private val remote: BankRemoteSource,
    private val loadBundledBank: suspend () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex() // 避免內建初始化與遠端同步同時寫入
    private val metaDao = db.bankMetaDao()
    private val questionDao = db.questionDao()

    val meta: Flow<BankMetaEntity?> = metaDao.observe()

    suspend fun ensureSeeded(): SyncOutcome = mutex.withLock {
        val text = try {
            loadBundledBank()
        } catch (e: Exception) {
            return SyncOutcome.Failed("讀取內建題庫失敗：${e.message}")
        }
        applyIfNewer(text, etag = null)
    }

    suspend fun syncRemote(): SyncOutcome = mutex.withLock {
        val local = metaDao.get()
        when (val result = remote.fetch(local?.etag)) {
            is FetchResult.Failed -> SyncOutcome.Failed(result.reason)
            FetchResult.NotModified -> {
                if (local == null) return SyncOutcome.Failed("本機沒有題庫")
                metaDao.upsert(local.copy(lastCheckedAt = clock()))
                SyncOutcome.UpToDate(local.version)
            }
            is FetchResult.Fetched -> applyIfNewer(result.body, result.etag)
        }
    }

    private suspend fun applyIfNewer(text: String, etag: String?): SyncOutcome {
        val parsed = try {
            BankParser.parse(text)
        } catch (e: Exception) {
            return SyncOutcome.Failed("題庫格式錯誤：${e.message}")
        }
        val bank = parsed.bank
        val local = metaDao.get()

        if (!BankVersionPolicy.shouldApply(local?.version, bank.version)) {
            // local 不可能是 null（null 時 shouldApply 一定是 true），!! 是告訴編譯器「我確定不是 null」
            metaDao.upsert(local!!.copy(etag = etag ?: local.etag, lastCheckedAt = clock()))
            return SyncOutcome.UpToDate(local.version)
        }

        val refs = questionDao.getRefs().map { LocalQuestionRef(it.id, it.contentHash, it.active) }
        val plan = BankMerger.plan(refs, bank.questions)

        // 題目、移除標記、版本號在同一個交易：要嘛全部成功，要嘛全部不變
        db.withTransaction {
            questionDao.upsertAll(plan.upserts.map { it.toEntity(bank.version) })
            // SQLite 一次最多 999 個參數，分批處理
            plan.retiredIds.chunked(500).forEach { questionDao.deactivate(it) }
            metaDao.upsert(
                BankMetaEntity(
                    version = bank.version,
                    updatedAt = bank.updatedAt,
                    etag = etag ?: local?.etag,
                    lastCheckedAt = clock(),
                )
            )
        }
        return SyncOutcome.Updated(
            fromVersion = local?.version,
            toVersion = bank.version,
            added = plan.addedIds.size + plan.reactivatedIds.size,
            updated = plan.updatedIds.size,
            retired = plan.retiredIds.size,
            skipped = parsed.issues.count { it.severity == Severity.ERROR },
        )
    }
}
