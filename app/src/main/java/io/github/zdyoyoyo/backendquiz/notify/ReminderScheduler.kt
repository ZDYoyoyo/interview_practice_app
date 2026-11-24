package io.github.zdyoyoyo.backendquiz.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.zdyoyoyo.backendquiz.data.prefs.ReminderSettings
import io.github.zdyoyoyo.backendquiz.domain.reminder.ReminderSchedule
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * 排程每日提醒：每 24 小時執行一次的 PeriodicWork，第一次的延遲算到「下一個提醒時間」。
 *
 * 每次 App 開啟或設定改變時都重新排程（CANCEL_AND_REENQUEUE），
 * 讓實際時間不會因為長期累積的誤差越飄越遠。
 */
class ReminderScheduler(
    private val context: Context,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun apply(settings: ReminderSettings) {
        if (!settings.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = ReminderSchedule.delayUntilNext(now(), settings.time)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    /** 設定頁的「立即測試通知」：馬上執行一次，而且一定會發通知 */
    fun testNow() {
        workManager.enqueue(
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(workDataOf(ReminderWorker.KEY_FORCE to true))
                .build()
        )
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
    }
}
