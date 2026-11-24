package io.github.zdyoyoyo.backendquiz.ui.mistakes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.backendquiz.AppContainer
import io.github.zdyoyoyo.backendquiz.data.repository.MistakeItem
import io.github.zdyoyoyo.backendquiz.domain.mistakes.MistakePolicy
import io.github.zdyoyoyo.backendquiz.domain.model.Domain
import io.github.zdyoyoyo.backendquiz.ui.text.TextSegment
import io.github.zdyoyoyo.backendquiz.ui.text.TextSegments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(container: AppContainer, onBack: () -> Unit, onRedo: () -> Unit) {
    val vm: MistakesViewModel = viewModel(
        factory = viewModelFactory { initializer { MistakesViewModel(container.practiceRepository) } },
    )
    val items by vm.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("錯題本") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("目前沒有錯題 👍", style = MaterialTheme.typography.titleMedium)
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onRedo, modifier = Modifier.fillMaxWidth()) { Text("重做錯題（${list.size}）") }
                    Text(
                        "連續答對 ${MistakePolicy.STREAK_TO_MASTER} 次會自動移出",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                // LazyColumn ≈ RecyclerView：只畫出看得到的項目
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.record.questionId }) { item ->
                        MistakeCard(item, onRemove = { vm.remove(item.record.questionId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeCard(item: MistakeItem, onRemove: () -> Unit) {
    // 清單只顯示題幹的第一段文字（跳過程式碼）
    val preview = TextSegments.parse(item.stem).filterIsInstance<TextSegment.Plain>().firstOrNull()?.text ?: item.stem
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(Domain.fromCode(item.domainCode).displayName, style = MaterialTheme.typography.labelMedium)
            Text(preview, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "答錯 ${item.record.wrongCount} 次 · 連對 ${item.record.correctStreak}/${MistakePolicy.STREAK_TO_MASTER}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) { Text("移出") }
            }
        }
    }
}
