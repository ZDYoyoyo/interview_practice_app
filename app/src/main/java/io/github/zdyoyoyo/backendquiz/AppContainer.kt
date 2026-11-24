package io.github.zdyoyoyo.backendquiz

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.zdyoyoyo.backendquiz.data.local.AppDatabase
import io.github.zdyoyoyo.backendquiz.data.prefs.SettingsStore
import io.github.zdyoyoyo.backendquiz.data.remote.OkHttpBankRemoteSource
import io.github.zdyoyoyo.backendquiz.data.repository.BankSyncRepository
import io.github.zdyoyoyo.backendquiz.data.repository.PracticeRepository
import io.github.zdyoyoyo.backendquiz.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 手動依賴注入：整個 App 共用的物件都在這裡建立一次。
 * 角色 ≈ Spring 的 ApplicationContext，只是全部手寫、看得見。
 * `by lazy` = 第一次用到才建立（且執行緒安全），≈ Java 的 lazy holder 寫法。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    }

    /** DataStore 同一個檔案在整個 App 只能有一個實例，所以放在 container 裡共用 */
    val settingsStore: SettingsStore by lazy {
        SettingsStore(PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") })
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }

    val bankSyncRepository: BankSyncRepository by lazy {
        BankSyncRepository(
            db = database,
            remote = OkHttpBankRemoteSource(httpClient, BuildConfig.BANK_URL),
            loadBundledBank = {
                withContext(Dispatchers.IO) {
                    appContext.assets.open("questions.json").bufferedReader().use { it.readText() }
                }
            },
            // 不管是 App 開啟時還是背景提醒時下載到新版，都記錄一則更新提示，首頁會顯示
            onUpdated = { outcome -> outcome.toNotice()?.let { settingsStore.addBankNotice(it) } },
        )
    }

    val practiceRepository: PracticeRepository by lazy { PracticeRepository(database) }
}
