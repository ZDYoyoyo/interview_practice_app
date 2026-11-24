package io.github.zdyoyoyo.backendquiz.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.repository.AnswerOutcome
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionType
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 畫面需要的所有狀態集中成一個不可變物件；要改就 copy() 出新的一份。 */
data class QuizUiState(
    val loading: Boolean = true,
    val mode: QuizMode = QuizMode.RANDOM,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: Set<String> = emptySet(),
    val submitting: Boolean = false,
    val outcome: AnswerOutcome? = null,
    val correctCount: Int = 0,
    val finished: Boolean = false,
) {
    val current: Question? get() = questions.getOrNull(index)
    val submitted: Boolean get() = outcome != null
    val isLast: Boolean get() = index == questions.lastIndex
}

class QuizViewModel(
    private val practice: PracticeRepository,
    savedStateHandle: SavedStateHandle, // 導覽參數（mode/domains/count）從這裡拿
) : ViewModel() {

    private val mode = QuizMode.fromCode(savedStateHandle.get<String>("mode"))
    private val domains: Set<String> = savedStateHandle.get<String>("domains").orEmpty()
        .split(',').filter { it.isNotBlank() }.toSet()
    private val count: Int = savedStateHandle.get<Int>("count") ?: 10

    private val _state = MutableStateFlow(QuizUiState(mode = mode))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val questions = practice.questionsForSession(mode, domains, count)
            _state.update { it.copy(loading = false, questions = questions, finished = questions.isEmpty()) }
        }
    }

    fun toggleOption(key: String) = _state.update { s ->
        val q = s.current ?: return@update s
        if (s.submitted) return@update s
        val newSelection = when (q.type) {
            QuestionType.SINGLE -> setOf(key)
            QuestionType.MULTI -> if (key in s.selected) s.selected - key else s.selected + key
        }
        s.copy(selected = newSelection)
    }

    fun submit() {
        val s = _state.value
        val q = s.current ?: return
        if (s.selected.isEmpty() || s.submitted || s.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val outcome = practice.recordAnswer(q, s.selected, mode)
            _state.update {
                it.copy(
                    submitting = false,
                    outcome = outcome,
                    correctCount = it.correctCount + if (outcome.correct) 1 else 0,
                )
            }
        }
    }

    fun next() = _state.update { s ->
        if (s.isLast) s.copy(finished = true)
        else s.copy(index = s.index + 1, selected = emptySet(), outcome = null)
    }
}
