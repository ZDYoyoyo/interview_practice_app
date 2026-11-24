package io.github.zdyoyoyo.backendquiz.domain.reminder

data class ReminderMessage(val title: String, val text: String)

/**
 * 決定每日提醒的內容。每天一定會發一則，內容依情況不同：
 *
 * - 有待複習的題目 → 「今天有 N 題該複習」
 * - 沒有待複習、今天已經練習過 → 「今天的複習都完成了」
 * - 沒有待複習、今天還沒練習 → 「今天還沒練習」
 * 有錯題或題庫更新時，會附在內文。
 */
object ReminderPolicy {

    fun build(
        dueCount: Int,
        answeredToday: Int,
        openMistakes: Int,
        bankUpdate: String? = null,
    ): ReminderMessage {
        val title = when {
            dueCount > 0 -> "今天有 $dueCount 題該複習"
            answeredToday > 0 -> "今天的複習都完成了"
            else -> "今天還沒練習"
        }
        val lines = buildList {
            add(
                when {
                    dueCount > 0 -> "花幾分鐘複習，記得更牢。"
                    answeredToday > 0 -> "做得好，今天已經練習 $answeredToday 題，想多練可以做幾題新題目。"
                    else -> "來做幾題後端面試題吧。"
                }
            )
            if (openMistakes > 0) add("錯題本還有 $openMistakes 題。")
            if (bankUpdate != null) add(bankUpdate)
        }
        return ReminderMessage(title, lines.joinToString(" "))
    }
}
