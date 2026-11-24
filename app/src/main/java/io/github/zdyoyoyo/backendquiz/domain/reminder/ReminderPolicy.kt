package io.github.zdyoyoyo.backendquiz.domain.reminder

data class ReminderMessage(val title: String, val text: String)

/**
 * 決定每日提醒要說什麼，或乾脆不提醒。
 *
 * - 有待複習的題目 → 一定提醒
 * - 沒有待複習、今天也還沒練習 → 提醒做新題目
 * - 沒有待複習、今天已經練習過 → 不打擾（除非有題庫更新要告訴你）
 */
object ReminderPolicy {

    fun build(
        dueCount: Int,
        answeredToday: Int,
        openMistakes: Int,
        bankUpdate: String? = null,
        force: Boolean = false,
    ): ReminderMessage? {
        if (!force && dueCount == 0 && answeredToday > 0 && bankUpdate == null) return null

        val title = when {
            dueCount > 0 -> "今天有 $dueCount 題該複習"
            answeredToday > 0 -> "今天的複習都完成了"
            else -> "今天還沒練習"
        }
        val lines = buildList {
            add(
                when {
                    dueCount > 0 -> "花幾分鐘複習，記得更牢。"
                    answeredToday > 0 -> "做得好，今天已經練習 $answeredToday 題。"
                    else -> "來做幾題後端面試題吧。"
                }
            )
            if (openMistakes > 0) add("錯題本還有 $openMistakes 題。")
            if (bankUpdate != null) add(bankUpdate)
        }
        return ReminderMessage(title, lines.joinToString(" "))
    }
}
