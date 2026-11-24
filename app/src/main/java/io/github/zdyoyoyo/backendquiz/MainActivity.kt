package io.github.zdyoyoyo.backendquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.zdyoyoyo.backendquiz.ui.navigation.AppNavHost
import io.github.zdyoyoyo.backendquiz.ui.theme.BackendQuizTheme

/** 單一 Activity：所有畫面都是 Compose，畫面切換交給 Navigation。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuizApp).container
        setContent {
            BackendQuizTheme {
                AppNavHost(container)
            }
        }
    }
}
