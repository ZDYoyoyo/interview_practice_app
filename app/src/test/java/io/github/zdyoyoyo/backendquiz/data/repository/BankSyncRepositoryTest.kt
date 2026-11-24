package io.github.zdyoyoyo.backendquiz.data.repository

import io.github.zdyoyoyo.backendquiz.TestQuestions.bankJson
import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.data.remote.FetchResult
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankSyncRepositoryTest : RepositoryTestBase() {

    private val q1 = question("q1", stem = "q1 舊題幹")
    private val q2 = question("q2")
    private val q3 = question("q3")

    @Test
    // 首次啟動用內建題庫初始化
    fun `first launch seeds from bundled bank`() = runTest {
        bundled = bankJson(1, q1, q2)
        val outcome = assertIs<SyncOutcome.Updated>(sync.ensureSeeded())
        assertEquals(null, outcome.fromVersion)
        assertEquals(2, outcome.added)
        assertEquals(2, practice.observeActiveCount().first())
    }

    @Test
    // 遠端版本相同時不會覆寫，但會記住 ETag
    fun `same remote version is not applied but etag is kept`() = runTest {
        bundled = bankJson(1, q1)
        sync.ensureSeeded()
        remote.next = FetchResult.Fetched(bankJson(1, q1, q2), etag = "\"e1\"")
        assertIs<SyncOutcome.UpToDate>(sync.syncRemote())
        assertEquals(1, practice.observeActiveCount().first())

        // 下次請求會帶上 ETag；伺服器回 304
        remote.next = FetchResult.NotModified
        assertIs<SyncOutcome.UpToDate>(sync.syncRemote())
        assertEquals("\"e1\"", remote.lastEtag)
    }

    @Test
    // 網路失敗時保留本機題庫
    fun `network failure keeps local bank`() = runTest {
        bundled = bankJson(1, q1)
        sync.ensureSeeded()
        remote.next = FetchResult.Failed("timeout")
        assertIs<SyncOutcome.Failed>(sync.syncRemote())
        assertEquals(1, practice.observeActiveCount().first())
    }

    /**
     * 核心需求：題庫更新後，舊的作答紀錄與錯題仍然對得上。
     * v1: q1, q2  →  v2: q1(內容更新), q3(新增)，q2 被移除
     */
    @Test
    // 題庫更新後舊紀錄的對應
    fun `records survive bank update`() = runTest {
        bundled = bankJson(1, q1, q2)
        sync.ensureSeeded()

        // v1 時答錯 q1、q2 → 都進錯題本
        practice.recordAnswer(q1, setOf("B"), QuizMode.RANDOM)
        practice.recordAnswer(q2, setOf("B"), QuizMode.RANDOM)
        assertEquals(2, practice.observeOpenMistakes().first().size)

        // 下載 v2
        remote.next = FetchResult.Fetched(bankJson(2, q1.copy(stem = "q1 新題幹"), q3), etag = null)
        val outcome = assertIs<SyncOutcome.Updated>(sync.syncRemote())
        assertEquals(1, outcome.added)
        assertEquals(1, outcome.updated)
        assertEquals(1, outcome.retired)

        // q1：作答紀錄還在、錯題還在，而且顯示的是新題幹
        assertEquals(1, db.attemptDao().forQuestion("q1").size)
        val mistakes = practice.observeOpenMistakes().first()
        assertEquals(listOf("q1"), mistakes.map { it.record.questionId })
        assertEquals("q1 新題幹", mistakes.single().stem)

        // q2：本機紀錄保留，但不再出現在錯題本，也不會被出題
        assertEquals(1, db.attemptDao().forQuestion("q2").size)
        assertTrue(db.mistakeDao().get("q2") != null)
        val pool = practice.questionsForSession(QuizMode.RANDOM, emptySet(), 100).map { it.id }.toSet()
        assertEquals(setOf("q1", "q3"), pool)

        // q2 之後又回到題庫（v3）→ 錯題紀錄自動接回來
        remote.next = FetchResult.Fetched(bankJson(3, q1, q2, q3), etag = null)
        sync.syncRemote()
        assertEquals(setOf("q1", "q2"), practice.observeOpenMistakes().first().map { it.record.questionId }.toSet())
    }

    @Test
    // 內建題庫比本機舊時，不會蓋掉已下載的新版
    fun `older bundled bank does not overwrite newer download`() = runTest {
        bundled = bankJson(1, q1)
        sync.ensureSeeded()
        remote.next = FetchResult.Fetched(bankJson(5, q1, q2), etag = null)
        sync.syncRemote()

        // App 重新啟動，再次讀內建 v1
        assertIs<SyncOutcome.UpToDate>(sync.ensureSeeded())
        assertEquals(5, sync.meta.first()!!.version)
        assertEquals(2, practice.observeActiveCount().first())
    }
}
