package io.github.zdyoyoyo.backendquiz.domain.reminder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPolicyTest {

    // 有待複習 → 一定提醒（即使今天練習過）
    @Test
    fun `due questions always notify`() {
        val m = assertNotNull(ReminderPolicy.build(dueCount = 4, answeredToday = 10, openMistakes = 2))
        assertEquals("今天有 4 題該複習", m.title)
        assertTrue("錯題本還有 2 題" in m.text)
    }

    // 沒有待複習、今天還沒練習 → 提醒做新題目
    @Test
    fun `nothing due and not practiced today notifies`() {
        assertEquals("今天還沒練習", ReminderPolicy.build(0, 0, 0)!!.title)
    }

    // 沒有待複習、今天已練習 → 不打擾
    @Test
    fun `nothing due and already practiced stays quiet`() {
        assertNull(ReminderPolicy.build(dueCount = 0, answeredToday = 5, openMistakes = 3))
    }

    // 但有題庫更新時還是會通知
    @Test
    fun `bank update is always worth a notification`() {
        val m = assertNotNull(ReminderPolicy.build(0, 5, 0, bankUpdate = "題庫已更新到 v2：新增 3 題"))
        assertTrue("新增 3 題" in m.text)
    }

    // 測試按鈕：強制通知
    @Test
    fun `force always returns a message`() {
        assertNotNull(ReminderPolicy.build(0, 5, 0, force = true))
    }
}
