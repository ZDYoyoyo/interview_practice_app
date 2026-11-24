package io.github.zdyoyoyo.backendquiz.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.backendquiz.AppContainer
import io.github.zdyoyoyo.backendquiz.domain.model.Domain
import io.github.zdyoyoyo.backendquiz.domain.stats.DomainStat
import io.github.zdyoyoyo.backendquiz.domain.stats.StatsCalculator
import io.github.zdyoyoyo.backendquiz.domain.stats.StatsSummary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(container: AppContainer, onBack: () -> Unit, onPracticeDomain: (Domain) -> Unit) {
    val vm: StatsViewModel = viewModel(
        factory = viewModelFactory { initializer { StatsViewModel(container.practiceRepository) } },
    )
    val summary by vm.summary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("統計") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        val s = summary
        when {
            s == null -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            s.totalAttempts == 0 -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("還沒有作答紀錄，先去做幾題吧", style = MaterialTheme.typography.titleMedium)
            }
            else -> StatsContent(s, Modifier.padding(padding), onPracticeDomain)
        }
    }
}

@Composable
private fun StatsContent(s: StatsSummary, modifier: Modifier, onPracticeDomain: (Domain) -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("整體", style = MaterialTheme.typography.titleMedium)
                Text("正確率 ${percent(s.overallAccuracy)}", style = MaterialTheme.typography.headlineSmall)
                Text("累計作答 ${s.totalAttempts} 次，答對 ${s.totalCorrect} 次", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("最弱的 ${StatsCalculator.WEAKEST_COUNT} 個領域", style = MaterialTheme.typography.titleMedium)
                if (s.weakest.isEmpty()) {
                    Text(
                        "目前沒有弱點領域。作答 ${StatsCalculator.MIN_ATTEMPTS_FOR_RANKING} 次以上、且有答錯的領域才會列入排名",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                s.weakest.forEachIndexed { i, d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}. ${d.domain.displayName}　${percent(d.accuracy)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onPracticeDomain(d.domain) }) { Text("練習") }
                    }
                }
            }
        }

        Text("各領域正確率", style = MaterialTheme.typography.titleMedium)
        s.domains.forEach { DomainRow(it, isWeak = it in s.weakest) }
    }
}

@Composable
private fun DomainRow(d: DomainStat, isWeak: Boolean) {
    val enough = d.total >= StatsCalculator.MIN_ATTEMPTS_FOR_RANKING
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(
                (if (isWeak) "⚠ " else "") + d.domain.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${d.correct}/${d.total}　${percent(d.accuracy)}" + if (enough) "" else "（樣本不足）",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LinearProgressIndicator(
            progress = { d.accuracy.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = if (isWeak) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            gapSize = 0.dp,
            drawStopIndicator = {}, // 不畫右端的小圓點（0% 時看起來像雜訊）
        )
    }
}

private fun percent(value: Double) = "${(value * 100).roundToInt()}%"
