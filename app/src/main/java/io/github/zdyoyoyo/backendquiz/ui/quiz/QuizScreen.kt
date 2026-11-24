package io.github.zdyoyoyo.backendquiz.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zdyoyoyo.backendquiz.AppContainer
import io.github.zdyoyoyo.backendquiz.data.repository.AnswerOutcome
import io.github.zdyoyoyo.backendquiz.domain.mistakes.MistakePolicy
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionOption
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import io.github.zdyoyoyo.backendquiz.ui.components.InlineCodeText
import io.github.zdyoyoyo.backendquiz.ui.components.RichText
import io.github.zdyoyoyo.backendquiz.ui.theme.LocalQuizColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(container: AppContainer, onExit: () -> Unit) {
    val vm: QuizViewModel = viewModel(
        factory = viewModelFactory {
            initializer { QuizViewModel(container.practiceRepository, createSavedStateHandle()) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    // 換題時捲回最上面
    LaunchedEffect(state.index) { scroll.scrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.loading -> "載入中"
                            state.finished -> "本輪結束"
                            else -> "${if (state.mode == QuizMode.MISTAKES) "錯題重做" else "做題"} ${state.index + 1} / ${state.questions.size}"
                        },
                    )
                },
                navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.Default.Close, "離開") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.finished -> Summary(state, onExit)
                else -> state.current?.let { q ->
                    Column(Modifier.fillMaxSize()) {
                        LinearProgressIndicator(
                            progress = { (state.index + 1f) / state.questions.size },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(scroll).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            QuestionHeader(q)
                            RichText(q.stem)
                            q.options.forEach { option ->
                                OptionRow(
                                    question = q,
                                    option = option,
                                    selected = option.key in state.selected,
                                    submitted = state.submitted,
                                    onClick = { vm.toggleOption(option.key) },
                                )
                            }
                            state.outcome?.let { ResultPanel(q, it) }
                        }
                        BottomAction(state, onSubmit = vm::submit, onNext = vm::next)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionHeader(q: Question) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AssistChip(onClick = {}, label = { Text(q.domain.displayName) })
        Text(
            "${if (q.type == QuestionType.MULTI) "多選" else "單選"} · " + "★".repeat(q.difficulty),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun OptionRow(
    question: Question,
    option: QuestionOption,
    selected: Boolean,
    submitted: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalQuizColors.current
    val isAnswer = option.key in question.answer
    // 送出後：正確答案標綠；選錯的標紅
    val (container, content) = when {
        submitted && isAnswer -> colors.correctContainer to colors.onCorrectContainer
        submitted && selected -> colors.wrongContainer to colors.onWrongContainer
        else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected && !submitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    // 單選用 selectable + RadioButton，多選用 toggleable + Checkbox（無障礙讀屏也會念出正確角色）
    val clickModifier = if (question.type == QuestionType.SINGLE) {
        Modifier.selectable(selected = selected, enabled = !submitted, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier.toggleable(value = selected, enabled = !submitted, role = Role.Checkbox, onValueChange = { onClick() })
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(if (selected && !submitted) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth().then(clickModifier),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (question.type == QuestionType.SINGLE) {
                RadioButton(selected = selected, onClick = null, enabled = !submitted)
            } else {
                Checkbox(checked = selected, onCheckedChange = null, enabled = !submitted)
            }
            InlineCodeText(
                "${option.key}. ${option.text}",
                modifier = Modifier.padding(vertical = 8.dp).weight(1f),
                color = content,
            )
        }
    }
}

@Composable
private fun ResultPanel(q: Question, outcome: AnswerOutcome) {
    val colors = LocalQuizColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (outcome.correct) colors.correctContainer else colors.wrongContainer,
        contentColor = if (outcome.correct) colors.onCorrectContainer else colors.onWrongContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (outcome.correct) "✓ 答對了" else "✗ 答錯了，正確答案：${q.answer.sorted().joinToString("、")}",
                style = MaterialTheme.typography.titleMedium,
            )
            val mistake = outcome.mistake
            when {
                outcome.justMastered -> Text("錯題過三關，已移出錯題本 🎉")
                mistake != null && !outcome.correct -> Text("已收錄到錯題本（累計答錯 ${mistake.wrongCount} 次）")
                mistake != null -> Text("錯題本連對 ${mistake.correctStreak} / ${MistakePolicy.STREAK_TO_MASTER}")
            }
        }
    }
    HorizontalDivider()
    Text("解析", style = MaterialTheme.typography.titleMedium)
    RichText(q.explanation, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun BottomAction(state: QuizUiState, onSubmit: () -> Unit, onNext: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            if (!state.submitted) {
                Button(
                    onClick = onSubmit,
                    enabled = state.selected.isNotEmpty() && !state.submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("送出") }
            } else {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isLast) "看結果" else "下一題")
                }
            }
        }
    }
}

@Composable
private fun Summary(state: QuizUiState, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.questions.isEmpty()) {
            Text(
                if (state.mode == QuizMode.MISTAKES) "錯題本是空的 👍" else "這個範圍沒有題目",
                style = MaterialTheme.typography.titleLarge,
            )
        } else {
            Text("答對 ${state.correctCount} / ${state.questions.size}", style = MaterialTheme.typography.headlineMedium)
            val rate = state.correctCount * 100 / state.questions.size
            Text("正確率 $rate%", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onExit) { Text("回首頁") }
    }
}
