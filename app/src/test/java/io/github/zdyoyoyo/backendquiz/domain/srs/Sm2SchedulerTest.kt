package io.github.zdyoyoyo.backendquiz.domain.srs

import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler.QUALITY_EASY
import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler.QUALITY_GOOD
import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler.QUALITY_HARD
import io.github.zdyoyoyo.backendquiz.domain.srs.Sm2Scheduler.QUALITY_WRONG
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class Sm2SchedulerTest {

    private fun review(state: ReviewState?, quality: Int, today: Long) =
        Sm2Scheduler.review(state, "q", quality, today)

    // 第一次答對：明天複習
    @Test
    fun `first correct answer is due tomorrow`() {
        val s = review(null, QUALITY_GOOD, today = 100)
        assertEquals(1, s.repetitions)
        assertEquals(1, s.intervalDays)
        assertEquals(101, s.dueEpochDay)
        assertEquals(100, s.lastReviewEpochDay)
        assertEquals(2.5, s.easiness, 1e-9)
    }

    // 依時複習都答對：間隔 1 → 6 → 15（6 × 2.5）→ 38（15 × 2.5 四捨五入）
    @Test
    fun `intervals grow 1 then 6 then multiply by easiness`() {
        var s = review(null, QUALITY_GOOD, today = 0)
        s = review(s, QUALITY_GOOD, today = s.dueEpochDay); assertEquals(6, s.intervalDays)
        s = review(s, QUALITY_GOOD, today = s.dueEpochDay); assertEquals(15, s.intervalDays)
        s = review(s, QUALITY_GOOD, today = s.dueEpochDay); assertEquals(38, s.intervalDays)
        assertEquals(4, s.repetitions)
        assertEquals(1 + 6 + 15 + 38L, s.dueEpochDay)
    }

    // 答錯：連續次數歸零、明天複習、EF 下降 0.32
    @Test
    fun `wrong answer resets repetitions and lowers easiness`() {
        var s = review(null, QUALITY_GOOD, today = 0)
        s = review(s, QUALITY_GOOD, today = 1)
        s = review(s, QUALITY_WRONG, today = 7)
        assertEquals(0, s.repetitions)
        assertEquals(1, s.intervalDays)
        assertEquals(8, s.dueEpochDay)
        assertEquals(2.18, s.easiness, 1e-9)
    }

    // 自評分數對 EF 的影響：秒答 +0.1、想了一下不變、猜對 −0.14
    @Test
    fun `quality changes easiness as in SM-2 formula`() {
        assertEquals(2.6, review(null, QUALITY_EASY, 0).easiness, 1e-9)
        assertEquals(2.5, review(null, QUALITY_GOOD, 0).easiness, 1e-9)
        assertEquals(2.36, review(null, QUALITY_HARD, 0).easiness, 1e-9)
    }

    // EF 不會低於 1.3
    @Test
    fun `easiness never drops below minimum`() {
        var s: ReviewState? = null
        for (day in 0L until 10) s = review(s, QUALITY_WRONG, today = day)
        assertEquals(Sm2Scheduler.MIN_EASINESS, s!!.easiness, 1e-9)
    }

    // 同一天再答對：排程不變（不能一天內刷高間隔）
    @Test
    fun `second correct answer on same day does not advance schedule`() {
        val s = review(null, QUALITY_GOOD, today = 5)
        assertSame(s, review(s, QUALITY_EASY, today = 5))
    }

    // 同一天再答錯：排回明天，但不重複扣 EF
    @Test
    fun `second wrong answer on same day reschedules without extra penalty`() {
        val first = review(null, QUALITY_WRONG, today = 5)
        val second = review(first, QUALITY_WRONG, today = 5)
        assertEquals(first.easiness, second.easiness, 1e-9)
        assertEquals(6, second.dueEpochDay)
        assertEquals(0, second.repetitions)
    }

    // 資料庫升級補上的舊題目（lastReview = null）照一般規則計算
    @Test
    fun `migrated state without last review is treated as new`() {
        val migrated = ReviewState("q", 2.5, 0, 0, dueEpochDay = 10, lastReviewEpochDay = null)
        val s = review(migrated, QUALITY_GOOD, today = 10)
        assertEquals(11, s.dueEpochDay)
    }

    @Test
    fun `quality out of range is rejected`() {
        assertFailsWith<IllegalArgumentException> { review(null, 6, 0) }
    }
}
