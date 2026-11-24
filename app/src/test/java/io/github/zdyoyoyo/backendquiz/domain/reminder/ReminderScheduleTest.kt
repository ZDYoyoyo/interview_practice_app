package io.github.zdyoyoyo.backendquiz.domain.reminder

import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class ReminderScheduleTest {
    private val taipei = ZoneId.of("Asia/Taipei")
    private val eight = LocalTime.of(20, 0)

    private fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, 24, h, m, s, 0, taipei)

    // 今天的提醒時間還沒到 → 今天
    @Test
    fun `before reminder time schedules today`() {
        assertEquals(Duration.ofHours(2).plusMinutes(30), ReminderSchedule.delayUntilNext(at(17, 30), eight))
    }

    // 已經過了 → 明天
    @Test
    fun `after reminder time schedules tomorrow`() {
        assertEquals(Duration.ofHours(23), ReminderSchedule.delayUntilNext(at(21, 0), eight))
    }

    // 剛好就是提醒時間 → 明天（避免重複觸發）
    @Test
    fun `exactly at reminder time schedules tomorrow`() {
        assertEquals(Duration.ofDays(1), ReminderSchedule.delayUntilNext(at(20, 0), eight))
    }

    // 秒數被忽略：20:00 的提醒，19:59:30 時只差 30 秒
    @Test
    fun `seconds are handled`() {
        assertEquals(Duration.ofSeconds(30), ReminderSchedule.delayUntilNext(at(19, 59, 30), eight))
    }

    // 日光節約時間開始的那晚（紐約 2026-03-08 凌晨跳過一小時）：牆上時間 20:00 → 20:00 只隔 23 小時
    @Test
    fun `daylight saving transition keeps wall clock time`() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 20, 0, 0, 0, ny)
        assertEquals(Duration.ofHours(23), ReminderSchedule.delayUntilNext(now, eight))
    }
}
