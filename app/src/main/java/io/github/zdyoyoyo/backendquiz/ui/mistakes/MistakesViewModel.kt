package io.github.zdyoyoyo.backendquiz.ui.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.repository.MistakeItem
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MistakesViewModel(private val practice: PracticeRepository) : ViewModel() {

    /** null = 還在載入 */
    val items: StateFlow<List<MistakeItem>?> = practice.observeOpenMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(questionId: String) {
        viewModelScope.launch { practice.removeFromMistakes(questionId) }
    }
}
