package io.github.zdyoyoyo.backendquiz.domain.bank

import org.junit.Test
import kotlin.test.assertEquals

class BankUpdateNoticeTest {

    @Test
    fun `message lists only non zero parts`() {
        assertEquals("題庫已更新到 v3：新增 5 題", BankUpdateNotice(2, 3, added = 5, updated = 0, retired = 0).message())
        assertEquals(
            "題庫已更新到 v3：新增 1 題、更新 2 題、移除 3 題（1 題格式有誤已略過）",
            BankUpdateNotice(2, 3, 1, 2, 3, skipped = 1).message(),
        )
    }

    // 只改了版本號、內容沒變
    @Test
    fun `message when nothing changed`() {
        assertEquals("題庫已更新到 v4（題目內容沒有變動）", BankUpdateNotice(3, 4, 0, 0, 0).message())
    }

    // 還沒看的提示遇到新的更新 → 合併
    @Test
    fun `merge keeps version range and sums counts`() {
        val merged = BankUpdateNotice(1, 2, 3, 1, 0).mergeWith(BankUpdateNotice(2, 3, 2, 0, 1))
        assertEquals(BankUpdateNotice(1, 3, 5, 1, 1), merged)
    }
}
