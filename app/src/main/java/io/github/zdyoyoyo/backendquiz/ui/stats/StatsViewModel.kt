package io.github.zdyoyoyo.backendquiz.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.domain.stats.StatsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(practice: PracticeRepository) : ViewModel() {
    /** null = 還在載入 */
    val summary: StateFlow<StatsSummary?> = practice.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
