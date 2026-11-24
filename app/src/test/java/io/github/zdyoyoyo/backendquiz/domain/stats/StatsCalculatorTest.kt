package io.github.zdyoyoyo.backendquiz.domain.stats

import io.github.zdyoyoyo.backendquiz.domain.model.Domain
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsCalculatorTest {

    private fun stat(id: String, domain: String, total: Int, correct: Int) = QuestionAttemptStat(id, domain, total, correct)

    // 各領域正確率由題目加總
    @Test
    fun `domain accuracy aggregates questions`() {
        val s = StatsCalculator.summarize(
            listOf(stat("a", "spring", 4, 3), stat("b", "spring", 6, 2), stat("c", "database", 2, 2))
        )
        val spring = s.domains.first { it.domain == Domain.SPRING }
        assertEquals(10, spring.total)
        assertEquals(5, spring.correct)
        assertEquals(0.5, spring.accuracy, 1e-9)
        assertEquals(2, spring.questionsAttempted)
        assertEquals(12, s.totalAttempts)
        assertEquals(7, s.totalCorrect)
        // 依 Domain 定義順序：SPRING 在 DATABASE 前面
        assertEquals(listOf(Domain.SPRING, Domain.DATABASE), s.domains.map { it.domain })
    }

    // 最弱 3 個：只算作答 >= 5 次的，正確率由低到高
    @Test
    fun `weakest three require minimum attempts and are sorted ascending`() {
        val s = StatsCalculator.summarize(
            listOf(
                stat("1", "spring", 10, 9),        // 90%
                stat("2", "database", 10, 3),      // 30%
                stat("3", "redis_mq", 10, 5),      // 50%
                stat("4", "algorithms", 10, 7),    // 70%
                stat("5", "security", 2, 0),       // 0% 但只有 2 次 → 不排名
            )
        )
        assertEquals(listOf(Domain.DATABASE, Domain.REDIS_MQ, Domain.ALGORITHMS), s.weakest.map { it.domain })
    }

    // 正確率相同時，作答多的排前面
    @Test
    fun `ties are broken by more attempts first`() {
        val s = StatsCalculator.summarize(listOf(stat("1", "spring", 5, 0), stat("2", "database", 8, 0)))
        assertEquals(listOf(Domain.DATABASE, Domain.SPRING), s.weakest.map { it.domain })
    }

    // 全對的領域不算弱點，即使排名靠後也不列入
    @Test
    fun `perfect domains are never weakest`() {
        val s = StatsCalculator.summarize(
            listOf(stat("1", "database", 10, 3), stat("2", "spring", 6, 6), stat("3", "java_basics", 6, 6))
        )
        assertEquals(listOf(Domain.DATABASE), s.weakest.map { it.domain })
    }

    @Test
    fun `no domain has enough attempts`() {
        val s = StatsCalculator.summarize(listOf(stat("1", "spring", 4, 0)))
        assertTrue(s.weakest.isEmpty())
        assertEquals(1, s.domains.size)
    }

    // 未知領域代碼歸到「其他」
    @Test
    fun `unknown domain codes are grouped as other`() {
        val s = StatsCalculator.summarize(listOf(stat("1", "quantum", 1, 1), stat("2", "blockchain", 1, 0)))
        assertEquals(Domain.OTHER, s.domains.single().domain)
        assertEquals(2, s.domains.single().total)
    }

    @Test
    fun `empty stats`() {
        val s = StatsCalculator.summarize(emptyList())
        assertEquals(0.0, s.overallAccuracy)
        assertTrue(s.domains.isEmpty())
    }
}
