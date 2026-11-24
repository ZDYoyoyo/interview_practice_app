package io.github.zdyoyoyo.backendquiz.notify

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.zdyoyoyo.backendquiz.data.prefs.ReminderSettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 用 WorkManager 官方的測試工具，驗證排程有沒有正確建立／取消 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = ZonedDateTime.of(2026, 11, 24, 18, 0, 0, 0, ZoneId.of("Asia/Taipei"))
    private val scheduler = ReminderScheduler(context, now = { now })

    @Before
    fun setUp() = WorkManagerTestInitHelper.initializeTestWorkManager(context)

    private fun infos() = WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get()

    @Test
    fun `enabling schedules periodic work for the next reminder time`() {
        scheduler.apply(ReminderSettings(enabled = true, hour = 20, minute = 30))
        val info = infos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        // 18:00 → 20:30，初始延遲 2.5 小時
        val delay = info.nextScheduleTimeMillis - System.currentTimeMillis()
        assertTrue(delay in (2 * 60 + 25) * 60_000L..(2 * 60 + 31) * 60_000L, "delay=$delay")
    }

    @Test
    fun `changing time replaces the existing work`() {
        scheduler.apply(ReminderSettings(true, 20, 0))
        scheduler.apply(ReminderSettings(true, 21, 0))
        assertEquals(1, infos().count { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun `disabling cancels the work`() {
        scheduler.apply(ReminderSettings(true, 20, 0))
        scheduler.apply(ReminderSettings(false, 20, 0))
        assertTrue(infos().all { it.state == WorkInfo.State.CANCELLED })
    }
}
