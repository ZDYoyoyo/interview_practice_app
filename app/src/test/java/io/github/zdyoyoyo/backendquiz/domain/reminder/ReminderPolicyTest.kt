package io.github.zdyoyoyo.backendquiz.domain.reminder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderPolicyTest {

    // 有待複習 → 提醒複習題數（即使今天練習過）
    @Test
    fun `due questions always notify`() {
        val m = ReminderPolicy.build(dueCount = 4, answeredToday = 10, openMistakes = 2)
        assertEquals("今天有 4 題該複習", m.title)
        assertTrue("錯題本還有 2 題" in m.text)
    }

    // 沒有待複習、今天還沒練習 → 提醒做新題目
    @Test
    fun `nothing due and not practiced today notifies`() {
        assertEquals("今天還沒練習", ReminderPolicy.build(0, 0, 0).title)
    }

    // 沒有待複習、今天已練習 → 仍然通知，告訴你今天完成了
    @Test
    fun `nothing due and already practiced still notifies`() {
        val m = ReminderPolicy.build(dueCount = 0, answeredToday = 5, openMistakes = 3)
        assertEquals("今天的複習都完成了", m.title)
        assertTrue("今天已經練習 5 題" in m.text)
        assertTrue("錯題本還有 3 題" in m.text)
    }

    // 題庫更新附在內文
    @Test
    fun `bank update is appended`() {
        val m = ReminderPolicy.build(0, 5, 0, bankUpdate = "題庫已更新到 v2：新增 3 題")
        assertTrue("新增 3 題" in m.text)
    }
}
