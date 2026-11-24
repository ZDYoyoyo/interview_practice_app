package io.github.zdyoyoyo.backendquiz.notify

import io.github.zdyoyoyo.backendquiz.data.repository.BankSyncRepository
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.data.repository.SyncOutcome
import io.github.zdyoyoyo.backendquiz.domain.reminder.ReminderMessage
import io.github.zdyoyoyo.backendquiz.domain.reminder.ReminderPolicy

/**
 * 每日提醒「要做的事」。和 Worker 分開，這樣不需要 WorkManager 也能測試。
 *   1. 順便檢查題庫更新（有新版本會一起寫進提醒）
 *   2. 查今天該複習幾題、今天做了幾題、錯題本幾題
 *   3. 交給 ReminderPolicy 組出通知內容並發送
 */
class ReminderRunner(
    private val practice: PracticeRepository,
    private val bankSync: BankSyncRepository,
    private val notify: (ReminderMessage) -> Unit,
) {
    suspend fun run(): ReminderMessage {
        val bankOutcome = try {
            bankSync.ensureSeeded()
            bankSync.syncRemote()
        } catch (e: Exception) {
            null // 沒網路也照樣提醒
        }
        val bankUpdate = (bankOutcome as? SyncOutcome.Updated)?.toNotice()?.message()

        val message = ReminderPolicy.build(
            dueCount = practice.dueCountToday(),
            answeredToday = practice.answeredTodayCount(),
            openMistakes = practice.openMistakeCount(),
            bankUpdate = bankUpdate,
        )
        notify(message)
        return message
    }
}
