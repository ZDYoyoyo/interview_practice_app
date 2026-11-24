package io.github.zdyoyoyo.backendquiz.domain.bank

/**
 * 題庫版本比對規則：只有「比本機新」才套用。
 *
 * - 本機沒有題庫（第一次啟動）→ 套用
 * - 新版本號 > 本機 → 套用
 * - 相同或更舊 → 不套用（避免題庫被意外倒退）
 */
object BankVersionPolicy {
    fun shouldApply(localVersion: Int?, incomingVersion: Int): Boolean =
        localVersion == null || incomingVersion > localVersion
}
