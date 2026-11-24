package io.github.zdyoyoyo.backendquiz.ui.text

import org.junit.Test
import kotlin.test.assertEquals

class TextSegmentsTest {

    @Test
    // 切出文字與程式碼區塊，保留縮排
    fun `splits text and code blocks keeping indentation`() {
        val src = "說明文字\n```java\nif (x) {\n    y();\n}\n```\n結尾"
        assertEquals(
            listOf(
                TextSegment.Plain("說明文字"),
                TextSegment.Code("if (x) {\n    y();\n}", "java"),
                TextSegment.Plain("結尾"),
            ),
            TextSegments.parse(src),
        )
    }

    @Test
    // 沒有關閉的程式碼區塊也當作程式碼
    fun `unclosed fence is still code`() {
        assertEquals(listOf(TextSegment.Code("code()", null)), TextSegments.parse("```\ncode()"))
    }

    @Test
    // 純文字
    fun `plain text only`() {
        assertEquals(listOf(TextSegment.Plain("hello")), TextSegments.parse("hello"))
    }

    @Test
    // 行內程式碼
    fun `inline code`() {
        assertEquals(
            listOf(InlineSpan("用 ", false), InlineSpan("HashMap", true), InlineSpan(" 存", false)),
            TextSegments.inlineSpans("用 `HashMap` 存"),
        )
    }

    @Test
    // 不成對的反引號當一般字元
    fun `unmatched backtick is literal`() {
        assertEquals(
            listOf(InlineSpan("a ", false), InlineSpan("b", true), InlineSpan(" c `d", false)),
            TextSegments.inlineSpans("a `b` c `d"),
        )
    }
}
