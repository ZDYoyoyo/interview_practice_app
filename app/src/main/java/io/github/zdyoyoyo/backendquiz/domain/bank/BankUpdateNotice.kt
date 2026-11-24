package io.github.zdyoyoyo.backendquiz.domain.bank

import kotlinx.serialization.Serializable

/**
 * 「題庫有更新」的提示內容。會存起來，直到使用者在首頁按「知道了」。
 * @Serializable：用 JSON 存進 DataStore。
 */
@Serializable
data class BankUpdateNotice(
    val fromVersion: Int,
    val toVersion: Int,
    val added: Int,
    val updated: Int,
    val retired: Int,
    val skipped: Int = 0,
) {
    /**
     * 使用者還沒看到上一則提示，又來了一次更新 → 合併成一則：
     * 版本從最早的到最新的，題數相加（同一題連續被更新兩次會算兩次，提示用途可以接受）。
     */
    fun mergeWith(newer: BankUpdateNotice) = BankUpdateNotice(
        fromVersion = minOf(fromVersion, newer.fromVersion),
        toVersion = maxOf(toVersion, newer.toVersion),
        added = added + newer.added,
        updated = updated + newer.updated,
        retired = retired + newer.retired,
        skipped = skipped + newer.skipped,
    )

    /** 例如「題庫已更新到 v3：新增 5 題、更新 2 題」 */
    fun message(): String {
        val parts = buildList {
            if (added > 0) add("新增 $added 題")
            if (updated > 0) add("更新 $updated 題")
            if (retired > 0) add("移除 $retired 題")
        }
        val body = if (parts.isEmpty()) "（題目內容沒有變動）" else "：" + parts.joinToString("、")
        val warn = if (skipped > 0) "（$skipped 題格式有誤已略過）" else ""
        return "題庫已更新到 v$toVersion$body$warn"
    }
}
