package io.github.zdyoyoyo.backendquiz.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class QuizSetupViewModel(practice: PracticeRepository) : ViewModel() {

    val domainCounts: StateFlow<Map<String, Int>> = practice.observeDomainCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    /** 空集合 = 全部領域 */
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _count = MutableStateFlow(10)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun toggle(code: String) = _selected.update { if (code in it) it - code else it + code }
    fun selectAll() { _selected.value = emptySet() }
    fun setCount(value: Int) { _count.value = value }
}
