package io.github.zdyoyoyo.backendquiz.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.zdyoyoyo.backendquiz.domain.bank.BankUpdateNotice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)
}

/**
 * 小量設定用 DataStore（SharedPreferences 的官方替代品）存放：
 * 非同步讀寫、不會卡住 UI 執行緒，讀取結果是 Flow，值一改畫面就自動更新。
 * 概念上像一個存成檔案的 Map<String, Any>。
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val BANK_NOTICE = stringPreferencesKey("bank_update_notice")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val reminder: Flow<ReminderSettings> = dataStore.data.map { p ->
        val default = ReminderSettings()
        ReminderSettings(
            enabled = p[Keys.REMINDER_ENABLED] ?: default.enabled,
            hour = p[Keys.REMINDER_HOUR] ?: default.hour,
            minute = p[Keys.REMINDER_MINUTE] ?: default.minute,
        )
    }

    suspend fun setReminder(settings: ReminderSettings) {
        dataStore.edit { p ->
            p[Keys.REMINDER_ENABLED] = settings.enabled
            p[Keys.REMINDER_HOUR] = settings.hour
            p[Keys.REMINDER_MINUTE] = settings.minute
        }
    }

    /** 尚未被使用者關閉的題庫更新提示；null = 沒有 */
    val bankNotice: Flow<BankUpdateNotice?> = dataStore.data.map { p ->
        p[Keys.BANK_NOTICE]?.let { runCatching { json.decodeFromString<BankUpdateNotice>(it) }.getOrNull() }
    }

    /** 記錄一次更新；如果上一則還沒看，就合併成一則 */
    suspend fun addBankNotice(notice: BankUpdateNotice) {
        // edit {} 是一個原子性的「讀 → 改 → 寫」，不會和其他寫入互相覆蓋
        dataStore.edit { p ->
            val previous = p[Keys.BANK_NOTICE]?.let { runCatching { json.decodeFromString<BankUpdateNotice>(it) }.getOrNull() }
            val merged = previous?.mergeWith(notice) ?: notice
            p[Keys.BANK_NOTICE] = json.encodeToString(BankUpdateNotice.serializer(), merged)
        }
    }

    suspend fun clearBankNotice() {
        dataStore.edit { it.remove(Keys.BANK_NOTICE) }
    }
}
