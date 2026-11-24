package io.github.zdyoyoyo.backendquiz.domain.quiz

import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.domain.stats.QuestionAttemptStat
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionPickerTest {

    private val weak = (1..10).map { question("weak-$it", domain = "database") }
    private val strong = (1..10).map { question("strong-$it", domain = "spring") }
    private val stats = weak.map { QuestionAttemptStat(it.id, "database", total = 4, correct = 0) } +
        strong.map { QuestionAttemptStat(it.id, "spring", total = 4, correct = 4) }

    // 弱點題的權重比強項題高；沒做過的題目介於中間
    @Test
    fun `weights favor weak domains and wrong questions`() {
        val w = WeaknessWeights(stats)
        val unseen = question("new", domain = "redis_mq")
        assertTrue(w.of(weak[0]) > w.of(unseen))
        assertTrue(w.of(unseen) > w.of(strong[0]))
    }

    // 抽出的題目不重複、數量正確
    @Test
    fun `pick returns distinct questions up to count`() {
        val picked = QuestionPicker(Random(1)).pickByWeakness(weak + strong, stats, 8)
        assertEquals(8, picked.size)
        assertEquals(8, picked.map { it.id }.toSet().size)
        assertEquals(5, QuestionPicker(Random(1)).pickByWeakness(weak.take(5), stats, 8).size)
    }

    // 多次抽樣統計：弱點題被抽中的次數明顯較多，但強項題仍有機會
    @Test
    fun `weak questions are picked much more often`() {
        val picker = QuestionPicker(Random(42))
        var weakCount = 0
        var strongCount = 0
        repeat(500) {
            picker.pickByWeakness(weak + strong, stats, 5).forEach {
                if (it.id.startsWith("weak")) weakCount++ else strongCount++
            }
        }
        assertTrue(weakCount > strongCount * 3, "weak=$weakCount strong=$strongCount")
        assertTrue(strongCount > 0)
    }
}
