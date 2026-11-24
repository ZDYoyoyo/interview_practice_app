package io.github.zdyoyoyo.backendquiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.repository.BankSyncRepository
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.data.repository.SyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bankVersion: Int? = null,
    val bankUpdatedAt: String = "",
    val questionCount: Int = 0,
    val mistakeCount: Int = 0,
    val syncStatus: String = "",
)

/**
 * ViewModel：保存畫面狀態，螢幕旋轉時不會被重建。
 * 畫面只「訂閱」uiState，所有邏輯在這裡。
 */
class HomeViewModel(
    private val bankSync: BankSyncRepository,
    practice: PracticeRepository,
) : ViewModel() {

    private val syncStatus = MutableStateFlow("")

    // combine：任何一個來源變動，就重新組出一份 HomeUiState
    val uiState: StateFlow<HomeUiState> = combine(
        bankSync.meta,
        practice.observeActiveCount(),
        practice.observeOpenMistakes(),
        syncStatus,
    ) { meta, count, mistakes, status ->
        HomeUiState(meta?.version, meta?.updatedAt.orEmpty(), count, mistakes.size, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        // viewModelScope：ViewModel 被銷毀時，裡面的協程會自動取消
        viewModelScope.launch {
            syncStatus.value = "正在檢查題庫更新…"
            bankSync.ensureSeeded()
            syncStatus.value = when (val result = bankSync.syncRemote()) {
                is SyncOutcome.UpToDate -> "題庫已是最新"
                is SyncOutcome.Updated -> "題庫已更新到 v${result.toVersion}"
                is SyncOutcome.Failed -> "無法連線，使用本機題庫（${result.reason}）"
            }
        }
    }
}
