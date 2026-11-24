package io.github.zdyoyoyo.backendquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.zdyoyoyo.backendquiz.data.local.AppDatabase
import io.github.zdyoyoyo.backendquiz.data.remote.BankRemoteSource
import io.github.zdyoyoyo.backendquiz.data.remote.FetchResult
import org.junit.After
import org.junit.Before
import java.time.ZoneOffset

/** 假的遠端：測試時直接指定下一次「下載」會拿到什麼 */
class FakeRemote : BankRemoteSource {
    var next: FetchResult = FetchResult.Failed("offline")
    var lastEtag: String? = null
    override suspend fun fetch(etag: String?): FetchResult {
        lastEtag = etag
        return next
    }
}

/** Robolectric 在 JVM 上模擬 Android，讓 Room 可以用記憶體資料庫測試，不需要模擬器 */
abstract class RepositoryTestBase {
    protected lateinit var db: AppDatabase
    protected val remote = FakeRemote()
    protected var bundled: String = """{"version":1,"updatedAt":"","questions":[]}"""
    /** 目前時間（毫秒）。用 UTC 計算日期，DAY 的整數倍就是「第幾天的 00:00」 */
    protected var now = 1_000L

    protected lateinit var sync: BankSyncRepository
    protected lateinit var practice: PracticeRepository

    protected fun goToDay(epochDay: Long) { now = epochDay * DAY + 1_000 }

    @Before
    fun setUpDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sync = BankSyncRepository(db, remote, loadBundledBank = { bundled }, clock = { now })
        practice = PracticeRepository(db, clock = { now }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDownDb() = db.close()
}

const val DAY = 24 * 60 * 60 * 1000L
