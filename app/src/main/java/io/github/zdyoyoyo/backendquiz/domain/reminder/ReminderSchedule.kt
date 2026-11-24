package io.github.zdyoyoyo.backendquiz.domain.reminder

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderSchedule {
    /**
     * 從 now 到「下一次提醒時間」還要多久。
     * 今天的提醒時間還沒到 → 今天；已經過了（或剛好就是現在）→ 明天。
     * 用 ZonedDateTime 計算，日光節約時間切換的日子也會正確。
     */
    fun delayUntilNext(now: ZonedDateTime, time: LocalTime): Duration {
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1).with(time)
        return Duration.between(now, next)
    }
}
