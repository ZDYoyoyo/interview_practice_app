package io.github.zdyoyoyo.backendquiz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.prefs.ReminderSettings
import io.github.zdyoyoyo.backendquiz.data.prefs.SettingsStore
import io.github.zdyoyoyo.backendquiz.notify.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsStore,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /** null = 還在讀取 */
    val reminder: StateFlow<ReminderSettings?> = settings.reminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setTime(hour: Int, minute: Int) = update { it.copy(hour = hour, minute = minute) }

    fun testNow() = scheduler.testNow()

    /** 存設定 → 依新設定重新排程 */
    private fun update(change: (ReminderSettings) -> ReminderSettings) {
        val current = reminder.value ?: return
        val next = change(current)
        viewModelScope.launch {
            settings.setReminder(next)
            scheduler.apply(next)
        }
    }
}
