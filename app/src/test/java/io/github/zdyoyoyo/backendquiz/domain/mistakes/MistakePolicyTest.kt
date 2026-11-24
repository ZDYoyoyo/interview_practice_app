package io.github.zdyoyoyo.backendquiz.domain.mistakes

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MistakePolicyTest {

    private fun answer(current: MistakeRecord?, correct: Boolean, now: Long = 100) =
        MistakePolicy.onAnswer(current, "q", correct, now)

    @Test
    // 答錯自動收錄
    fun `wrong answer is collected`() {
        val r = assertNotNull(answer(null, correct = false, now = 10))
        assertEquals(1, r.wrongCount)
        assertEquals(0, r.correctStreak)
        assertFalse(r.mastered)
        assertEquals(10, r.firstWrongAt)
    }

    @Test
    // 從沒錯過的題目答對，不收錄
    fun `correct answer on clean question is not collected`() {
        assertNull(answer(null, correct = true))
    }

    @Test
    // 連續答對三次才移出（過三關）
    fun `three correct in a row masters the mistake`() {
        var r = answer(null, correct = false)
        r = answer(r, correct = true); assertEquals(1, r!!.correctStreak); assertFalse(r.mastered)
        r = answer(r, correct = true); assertEquals(2, r!!.correctStreak); assertFalse(r.mastered)
        r = answer(r, correct = true); assertEquals(3, r!!.correctStreak); assertTrue(r.mastered)
    }

    @Test
    // 中途答錯，連對歸零並累加答錯次數
    fun `wrong answer resets streak and increments count`() {
        var r = answer(null, correct = false)
        r = answer(r, correct = true)
        r = answer(r, correct = true)
        r = answer(r, correct = false, now = 500)
        assertEquals(0, r!!.correctStreak)
        assertEquals(2, r.wrongCount)
        assertEquals(500, r.lastWrongAt)
        assertFalse(r.mastered)
    }

    @Test
    // 已移出的題目再答錯會重新收錄，保留歷史答錯次數
    fun `mastered question answered wrong is collected again`() {
        val mastered = MistakeRecord("q", wrongCount = 2, correctStreak = 3, mastered = true, firstWrongAt = 1, lastWrongAt = 2)
        val r = answer(mastered, correct = false, now = 900)!!
        assertFalse(r.mastered)
        assertEquals(3, r.wrongCount)
        assertEquals(1, r.firstWrongAt)
    }

    @Test
    // 已移出的題目答對，不再變動
    fun `mastered question answered correctly is unchanged`() {
        val mastered = MistakeRecord("q", 1, 3, true, 1, 1)
        assertNull(answer(mastered, correct = true))
    }
}
