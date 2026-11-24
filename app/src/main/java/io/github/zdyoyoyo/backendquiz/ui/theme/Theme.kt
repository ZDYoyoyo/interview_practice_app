package io.github.zdyoyoyo.backendquiz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** 對錯配色：Material 預設色盤沒有「正確綠」，自己定義，並支援深色模式。 */
@Immutable
data class QuizColors(
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val wrongContainer: Color,
    val onWrongContainer: Color,
    val codeBackground: Color,
)

private val LightQuizColors = QuizColors(
    correctContainer = Color(0xFFD7F2DC),
    onCorrectContainer = Color(0xFF0F5223),
    wrongContainer = Color(0xFFFADAD7),
    onWrongContainer = Color(0xFF7A1C14),
    codeBackground = Color(0xFFF1F3F5),
)

private val DarkQuizColors = QuizColors(
    correctContainer = Color(0xFF1E4B2A),
    onCorrectContainer = Color(0xFFBDEBC6),
    wrongContainer = Color(0xFF5C221D),
    onWrongContainer = Color(0xFFFFD4CF),
    codeBackground = Color(0xFF23272E),
)

val LocalQuizColors = staticCompositionLocalOf { LightQuizColors }

@Composable
fun BackendQuizTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        // Android 12+ 跟隨桌布配色（Material You）
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    CompositionLocalProvider(
        LocalQuizColors provides if (darkTheme) DarkQuizColors else LightQuizColors,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
