package io.github.zdyoyoyo.backendquiz.domain.bank

import io.github.zdyoyoyo.backendquiz.domain.model.Question
import java.security.MessageDigest

/**
 * 計算題目「內容指紋」，用來判斷同一個 id 的題目內容是否被更新過。
 * 外部流程不用維護任何修訂號，App 自己算。
 */
object ContentHasher {

    fun hash(q: Question): String {
        // 每個欄位寫成「長度:內容|」，避免 "ab"+"c" 和 "a"+"bc" 算出一樣的字串
        val canonical = buildString {
            field(q.domainCode)
            field(q.sourceTitle)
            field(q.type.code)
            field(q.stem)
            q.options.forEach { field(it.key); field(it.text) }
            q.answer.sorted().forEach { field(it) } // Set 沒有順序，排序後才穩定
            field(q.explanation)
            field(q.difficulty.toString())
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // 擴充函式（extension function）：替 StringBuilder「加」一個私有方法，Java 沒有這種語法
    private fun StringBuilder.field(value: String) {
        append(value.length).append(':').append(value).append('|')
    }
}
