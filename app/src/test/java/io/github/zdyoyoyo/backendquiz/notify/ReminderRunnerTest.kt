package io.github.zdyoyoyo.backendquiz.notify

import io.github.zdyoyoyo.backendquiz.TestQuestions.bankJson
import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.data.remote.FetchResult
import io.github.zdyoyoyo.backendquiz.data.repository.RepositoryTestBase
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import io.github.zdyoyoyo.backendquiz.domain.reminder.ReminderMessage
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderRunnerTest : RepositoryTestBase() {

    private val q1 = question("q1")
    private val q2 = question("q2")
    private val posted = mutableListOf<ReminderMessage>()
    private fun runner() = ReminderRunner(practice, sync, notify = { posted += it })

    // 昨天做過的題目今天到期 → 提醒今天該複習幾題
    @Test
    fun `notifies due count`() = runTest {
        bundled = bankJson(1, q1, q2)
        goToDay(100)
        sync.ensureSeeded()
        practice.recordAnswer(q1, setOf("A"), QuizMode.RANDOM)
        practice.recordAnswer(q2, setOf("B"), QuizMode.RANDOM)

        goToDay(101)
        runner().run()
        assertEquals("今天有 2 題該複習", posted.single().title)
        assertTrue("錯題本還有 1 題" in posted.single().text)
    }

    // 今天已經複習完 → 仍然通知（內容是「完成了」）
    @Test
    fun `notifies even when done for today`() = runTest {
        bundled = bankJson(1, q1)
        goToDay(100)
        sync.ensureSeeded()
        practice.recordAnswer(q1, setOf("A"), QuizMode.RANDOM)
        runner().run()
        assertEquals("今天的複習都完成了", posted.single().title)
    }

    // 背景檢查時下載到新題庫 → 通知裡附上更新內容，也會觸發首頁提示
    @Test
    fun `includes bank update from background sync`() = runTest {
        bundled = bankJson(1, q1)
        goToDay(100)
        sync.ensureSeeded()
        practice.recordAnswer(q1, setOf("A"), QuizMode.RANDOM)
        remote.next = FetchResult.Fetched(bankJson(2, q1, q2), etag = null)

        runner().run()
        assertTrue("題庫已更新到 v2：新增 1 題" in posted.single().text)
        assertEquals(1, updates.size)
    }

}
