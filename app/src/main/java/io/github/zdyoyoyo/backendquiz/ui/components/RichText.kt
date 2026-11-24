package io.github.zdyoyoyo.backendquiz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.zdyoyoyo.backendquiz.ui.text.TextSegment
import io.github.zdyoyoyo.backendquiz.ui.text.TextSegments
import io.github.zdyoyoyo.backendquiz.ui.theme.LocalQuizColors

/**
 * 顯示含程式碼的文字。@Composable 函式 = 一段 UI；參數改變時 Compose 會自動重畫。
 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
) {
    // remember：只有 text 改變才重新解析，避免每次重畫都做一次
    val segments = remember(text) { TextSegments.parse(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is TextSegment.Plain -> InlineCodeText(segment.text, style = style, color = color)
                is TextSegment.Code -> CodeBlock(segment.code)
            }
        }
    }
}

/**
 * 程式碼區塊：等寬字型、不自動換行（softWrap = false），
 * 外層 horizontalScroll 讓長行可以左右滑動。
 */
@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    Text(
        text = code,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        softWrap = false,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LocalQuizColors.current.codeBackground)
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    )
}

/** 一般文字，其中 `反引號` 包住的部分用等寬字型＋底色顯示。 */
@Composable
fun InlineCodeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
) {
    val codeBg = LocalQuizColors.current.codeBackground
    val annotated = remember(text, codeBg) {
        buildAnnotatedString {
            TextSegments.inlineSpans(text).forEach { span ->
                if (span.isCode) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 0.9.em, background = codeBg)) { append(span.text) }
                } else {
                    append(span.text)
                }
            }
        }
    }
    Text(annotated, modifier = modifier, style = style, color = color)
}
