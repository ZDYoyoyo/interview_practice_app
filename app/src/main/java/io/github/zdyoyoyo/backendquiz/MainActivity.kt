package io.github.zdyoyoyo.backendquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.zdyoyoyo.backendquiz.ui.navigation.AppNavHost
import io.github.zdyoyoyo.backendquiz.ui.theme.BackendQuizTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 單一 Activity：所有畫面都是 Compose，畫面切換交給 Navigation。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuizApp).container

        // 每次開 App 依照設定重新排程每日提醒（校正 WorkManager 長期累積的時間誤差）
        lifecycleScope.launch {
            runCatching { container.reminderScheduler.apply(container.settingsStore.reminder.first()) }
        }
        setContent {
            BackendQuizTheme {
                AppNavHost(container)
            }
        }
    }
}
