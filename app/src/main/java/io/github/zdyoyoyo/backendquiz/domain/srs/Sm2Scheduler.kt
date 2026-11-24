package io.github.zdyoyoyo.backendquiz.domain.srs

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 一題的間隔複習狀態。日期用 epochDay（1970-01-01 起算的天數，= LocalDate.toEpochDay()），
 * 存進資料庫只是一個整數，比較大小也最簡單。
 */
data class ReviewState(
    val questionId: String,
    /** 難易係數 EF：越大代表越熟，間隔成長越快。下限 1.3 */
    val easiness: Double,
    /** 目前間隔天數 */
    val intervalDays: Int,
    /** 連續「及格」次數；答錯歸零 */
    val repetitions: Int,
    /** 下次該複習的日期 */
    val dueEpochDay: Long,
    /** 最後一次複習的日期；null = 從沒複習過（例如資料庫升級時補上的舊題目） */
    val lastReviewEpochDay: Long?,
)

/**
 * SM-2 間隔重複演算法（SuperMemo 2，Piotr Woźniak 1987）。
 *
 * 評分 quality 0–5，App 的對應：
 *   答錯 = 2；答對後自評：秒答 = 5、想了一下 = 4（預設）、猜對 = 3
 *
 * 規則：
 *   quality >= 3（及格）：第 1 次間隔 1 天、第 2 次 6 天，之後 = 上次間隔 × EF
 *   quality <  3（不及格）：連續次數歸零，明天再複習
 *   EF' = EF + (0.1 − (5−q) × (0.08 + (5−q) × 0.02))，最低 1.3
 *     → q=5：+0.10　q=4：不變　q=3：−0.14　q=2：−0.32
 *
 * App 自己加的規則（避免同一天重複作答把排程「刷」上去）：
 *   同一天已經複習過的題目，再答對不改變排程；再答錯只把它排回明天，不重複扣 EF。
 */
object Sm2Scheduler {
    const val INITIAL_EASINESS = 2.5
    const val MIN_EASINESS = 1.3
    const val PASS_QUALITY = 3

    const val QUALITY_WRONG = 2
    const val QUALITY_HARD = 3   // 猜對
    const val QUALITY_GOOD = 4   // 想了一下（答對的預設值）
    const val QUALITY_EASY = 5   // 秒答

    fun review(current: ReviewState?, questionId: String, quality: Int, today: Long): ReviewState {
        require(quality in 0..5) { "quality 必須在 0..5，實際為 $quality" }

        if (current != null && current.lastReviewEpochDay == today) {
            return if (quality >= PASS_QUALITY) current
            else current.copy(repetitions = 0, intervalDays = 1, dueEpochDay = today + 1)
        }

        val state = current ?: ReviewState(questionId, INITIAL_EASINESS, 0, 0, today, null)

        // 間隔用「更新前」的 EF 計算（依原始 SM-2 的順序）
        val (repetitions, interval) = if (quality >= PASS_QUALITY) {
            when (state.repetitions) {
                0 -> 1 to 1
                1 -> 2 to 6
                else -> (state.repetitions + 1) to (state.intervalDays * state.easiness).roundToInt()
            }
        } else {
            0 to 1
        }

        val q = 5 - quality
        val easiness = max(MIN_EASINESS, state.easiness + (0.1 - q * (0.08 + q * 0.02)))

        return state.copy(
            easiness = easiness,
            intervalDays = interval,
            repetitions = repetitions,
            dueEpochDay = today + interval,
            lastReviewEpochDay = today,
        )
    }
}
