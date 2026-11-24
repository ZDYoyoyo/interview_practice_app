package io.github.zdyoyoyo.backendquiz.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.zdyoyoyo.backendquiz.domain.bank.BankUpdateNotice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** DataStore 的核心是純 JVM，不需要 Robolectric；用暫存資料夾當儲存位置 */
class SettingsStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun kotlinx.coroutines.test.TestScope.store() = SettingsStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("settings.preferences_pb").also { it.delete() } }
    )

    @Test
    fun `reminder defaults to off at 20 00`() = runTest {
        assertEquals(ReminderSettings(enabled = false, hour = 20, minute = 0), store().reminder.first())
    }

    @Test
    fun `reminder settings round trip`() = runTest {
        val s = store()
        s.setReminder(ReminderSettings(true, 7, 45))
        assertEquals(ReminderSettings(true, 7, 45), s.reminder.first())
    }

    // 題庫更新提示：未讀時再來一次更新會合併；按「知道了」後清除
    @Test
    fun `bank notices merge until cleared`() = runTest {
        val s = store()
        assertNull(s.bankNotice.first())
        s.addBankNotice(BankUpdateNotice(1, 2, 3, 0, 0))
        s.addBankNotice(BankUpdateNotice(2, 3, 1, 2, 0))
        assertEquals(BankUpdateNotice(1, 3, 4, 2, 0), s.bankNotice.first())
        s.clearBankNotice()
        assertNull(s.bankNotice.first())
    }
}
