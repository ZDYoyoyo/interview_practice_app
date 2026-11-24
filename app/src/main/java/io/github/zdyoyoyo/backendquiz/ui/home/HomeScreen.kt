package io.github.zdyoyoyo.backendquiz.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.backendquiz.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onStartQuiz: () -> Unit,
    onOpenMistakes: () -> Unit,
) {
    // 用 factory 把 Repository 傳進 ViewModel（手動 DI）
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.bankSyncRepository, container.practiceRepository) }
        },
    )
    // by：Kotlin 的委派語法，state 變了畫面會自動重畫
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("後端面試題複習") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("題庫", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.bankVersion == null) "載入中…"
                        else "v${state.bankVersion} · 共 ${state.questionCount} 題",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (state.bankUpdatedAt.isNotEmpty()) {
                        Text("更新時間 ${state.bankUpdatedAt}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(state.syncStatus, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = vm::refresh) { Text("重新檢查更新") }
                }
            }
            Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth()) { Text("開始做題") }
            OutlinedButton(onClick = onOpenMistakes, modifier = Modifier.fillMaxWidth()) {
                Text("錯題本（${state.mistakeCount}）")
            }
        }
    }
}
