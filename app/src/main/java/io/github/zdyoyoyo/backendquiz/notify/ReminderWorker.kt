package io.github.zdyoyoyo.backendquiz.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.zdyoyoyo.backendquiz.QuizApp

/**
 * WorkManager 在背景執行的工作。CoroutineWorker 的 doWork 是 suspend 函式，可以直接呼叫 Repository。
 * 即使 App 沒開、手機重開機，WorkManager 也會在時間到時執行它（時間可能因省電機制延後）。
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as QuizApp).container
        val runner = ReminderRunner(
            practice = container.practiceRepository,
            bankSync = container.bankSyncRepository,
            notify = { Notifications.showReminder(applicationContext, it) },
        )
        runner.run(force = inputData.getBoolean(KEY_FORCE, false))
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
