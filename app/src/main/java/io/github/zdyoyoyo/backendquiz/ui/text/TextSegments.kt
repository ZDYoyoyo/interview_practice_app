package io.github.zdyoyoyo.backendquiz.ui.text

/**
 * 把題幹／解析切成「一般文字」和「程式碼區塊」。
 * 純 Kotlin（不依賴 Compose），所以可以單元測試；畫面在 ui/components/RichText.kt。
 *
 * 支援的語法（Markdown 子集）：
 * - 程式碼區塊：```java ... ```（語言可省略）
 * - 行內程式碼：`code`
 */
sealed interface TextSegment {
    data class Plain(val text: String) : TextSegment
    data class Code(val code: String, val language: String?) : TextSegment
}

data class InlineSpan(val text: String, val isCode: Boolean)

object TextSegments {

    fun parse(source: String): List<TextSegment> {
        val result = mutableListOf<TextSegment>()
        val buffer = StringBuilder()
        var inCode = false
        var language: String? = null

        fun flush() { // 區域函式：可以讀寫外層的變數
            if (inCode) {
                val code = buffer.toString().trimEnd('\n')
                if (code.isNotEmpty()) result += TextSegment.Code(code, language)
            } else {
                val text = buffer.toString().trim('\n')
                if (text.isNotBlank()) result += TextSegment.Plain(text)
            }
            buffer.clear()
        }

        for (line in source.lines()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                flush()
                if (!inCode) language = trimmed.removePrefix("```").trim().ifEmpty { null }
                inCode = !inCode
                continue
            }
            buffer.append(line).append('\n') // 程式碼保留原本縮排
        }
        flush() // 沒關閉的 ``` 也當作程式碼收尾
        return result
    }

    /** 把一段文字依反引號切成一般文字與行內程式碼；不成對的反引號當一般字元。 */
    fun inlineSpans(text: String): List<InlineSpan> {
        val parts = text.split('`').toMutableList()
        if (parts.size % 2 == 0) { // 反引號數量是奇數 → 最後一個沒配對
            val last = parts.removeAt(parts.lastIndex)
            parts[parts.lastIndex] = parts.last() + "`" + last
        }
        return parts.mapIndexed { i, s -> InlineSpan(s, isCode = i % 2 == 1) }.filter { it.text.isNotEmpty() }
    }
}
