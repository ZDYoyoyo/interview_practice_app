package io.github.zdyoyoyo.backendquiz.domain.mistakes

data class MistakeRecord(
    val questionId: String,
    val wrongCount: Int,
    /** 進入錯題本後「連續答對」次數，答錯會歸零 */
    val correctStreak: Int,
    /** true = 已過三關，從錯題本移出（紀錄仍保留） */
    val mastered: Boolean,
    val firstWrongAt: Long,
    val lastWrongAt: Long,
)

/**
 * 錯題本規則：
 * 1. 答錯 → 收錄（已移出的題目再答錯會重新收回），連對次數歸零
 * 2. 在錯題本裡的題目答對 → 連對 +1；達到 [STREAK_TO_MASTER] 次就移出
 * 3. 從沒錯過的題目答對 → 不做任何事
 * 不管在哪個模式下作答都算（一般做題答對也能幫錯題「過關」）。
 */
object MistakePolicy {
    const val STREAK_TO_MASTER = 3

    /**
     * @param current 這題目前的錯題紀錄；null 表示從沒錯過
     * @return 更新後的紀錄；null 表示不需要寫入
     */
    fun onAnswer(current: MistakeRecord?, questionId: String, correct: Boolean, now: Long): MistakeRecord? {
        if (!correct) {
            return current?.copy(
                wrongCount = current.wrongCount + 1,
                correctStreak = 0,
                mastered = false,
                lastWrongAt = now,
            ) ?: MistakeRecord(
                questionId = questionId,
                wrongCount = 1,
                correctStreak = 0,
                mastered = false,
                firstWrongAt = now,
                lastWrongAt = now,
            )
        }
        if (current == null || current.mastered) return null

        val streak = current.correctStreak + 1
        return current.copy(correctStreak = streak, mastered = streak >= STREAK_TO_MASTER)
    }
}
