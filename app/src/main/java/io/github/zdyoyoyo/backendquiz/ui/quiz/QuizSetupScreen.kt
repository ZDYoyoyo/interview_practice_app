package io.github.zdyoyoyo.backendquiz.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import io.github.zdyoyoyo.backendquiz.domain.model.Domain

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizSetupScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onStart: (domains: Set<String>, count: Int) -> Unit,
) {
    val vm: QuizSetupViewModel = viewModel(
        factory = viewModelFactory { initializer { QuizSetupViewModel(container.practiceRepository) } },
    )
    val counts by vm.domainCounts.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()

    // 依 enum 順序列出「題庫裡有題目」的領域；未知代碼會被歸到 OTHER
    val available = Domain.entries.mapNotNull { domain ->
        val n = counts.filterKeys { Domain.fromCode(it) == domain }.values.sum()
        if (n > 0) domain to n else null
    }
    val poolSize = if (selected.isEmpty()) counts.values.sum()
    else counts.filterKeys { it in selected }.values.sum()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("選擇題目範圍") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("領域（不選 = 全部）", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selected.isEmpty(), onClick = vm::selectAll, label = { Text("全部") })
                available.forEach { (domain, n) ->
                    // OTHER 底下可能有多個未知代碼，一起選取
                    val codes = counts.keys.filter { Domain.fromCode(it) == domain }
                    FilterChip(
                        selected = codes.any { it in selected },
                        onClick = { codes.forEach(vm::toggle) },
                        label = { Text("${domain.displayName} ($n)") },
                    )
                }
            }

            Text("題數", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 50).forEach { n ->
                    FilterChip(selected = count == n, onClick = { vm.setCount(n) }, label = { Text("$n 題") })
                }
            }

            Text("可出題：$poolSize 題（隨機抽出）", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { onStart(selected, count) },
                enabled = poolSize > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("開始") }
        }
    }
}
