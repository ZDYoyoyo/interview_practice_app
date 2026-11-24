package io.github.zdyoyoyo.backendquiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zdyoyoyo.backendquiz.data.prefs.SettingsStore
import io.github.zdyoyoyo.backendquiz.data.repository.BankSyncRepository
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.data.repository.SyncOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bankVersion: Int? = null,
    val bankUpdatedAt: String = "",
    val questionCount: Int = 0,
    val mistakeCount: Int = 0,
    val dueCount: Int = 0,
    val syncStatus: String = "",
    /** 題庫更新提示，例如「題庫已更新到 v3：新增 5 題」；null = 沒有 */
    val bankNotice: String? = null,
)

/**
 * ViewModel：保存畫面狀態，螢幕旋轉時不會被重建。
 * 畫面只「訂閱」uiState，所有邏輯在這裡。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val bankSync: BankSyncRepository,
    private val practice: PracticeRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val syncStatus = MutableStateFlow("")

    /** 今天的日期；App 從背景回到前景時更新（跨過午夜時，「今天該複習」才會跟著變） */
    private val today = MutableStateFlow(practice.currentEpochDay())

    // flatMapLatest：today 一變，就改訂閱新日期的查詢（舊的查詢自動取消）
    private val dueCount = today.flatMapLatest { practice.observeDueCount(it) }

    // combine：任何一個來源變動，就重新組出一份 HomeUiState
    val uiState: StateFlow<HomeUiState> = combine(
        bankSync.meta,
        practice.observeActiveCount(),
        practice.observeOpenMistakes(),
        dueCount,
        syncStatus,
    ) { meta, count, mistakes, due, status ->
        HomeUiState(meta?.version, meta?.updatedAt.orEmpty(), count, mistakes.size, due, status)
    }.combine(settings.bankNotice) { state, notice ->
        // combine 最多一次接 5 個 Flow，第 6 個再接一層
        state.copy(bankNotice = notice?.message())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun dismissBankNotice() {
        viewModelScope.launch { settings.clearBankNotice() }
    }

    init {
        refresh()
    }

    fun onResume() {
        today.value = practice.currentEpochDay()
    }

    fun refresh() {
        // viewModelScope：ViewModel 被銷毀時，裡面的協程會自動取消
        viewModelScope.launch {
            syncStatus.value = "正在檢查題庫更新…"
            bankSync.ensureSeeded()
            syncStatus.value = when (val result = bankSync.syncRemote()) {
                is SyncOutcome.UpToDate -> "題庫已是最新"
                is SyncOutcome.Updated -> "題庫已更新到 v${result.toVersion}" // 詳細數字顯示在上方的提示卡
                is SyncOutcome.Failed -> "無法連線，使用本機題庫（${result.reason}）"
            }
        }
    }
}
