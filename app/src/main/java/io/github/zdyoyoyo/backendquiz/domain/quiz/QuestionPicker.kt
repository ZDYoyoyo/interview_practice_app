package io.github.zdyoyoyo.backendquiz.domain.quiz

import io.github.zdyoyoyo.backendquiz.domain.model.Domain
import io.github.zdyoyoyo.backendquiz.domain.model.Question
import io.github.zdyoyoyo.backendquiz.domain.stats.QuestionAttemptStat
import kotlin.math.pow
import kotlin.random.Random

/**
 * 出題策略。Random 從建構子注入，測試時可以傳固定種子，讓結果可重現。
 */
class QuestionPicker(private val random: Random = Random.Default) {

    fun pickRandom(pool: List<Question>, count: Int): List<Question> =
        pool.shuffled(random).take(count.coerceAtLeast(0))

    /**
     * 依弱點加權隨機抽題（不重複）。
     * 越弱的題目被抽中的機率越高，但強項也有機會出現，不會完全只出同一批題目。
     *
     * 加權抽樣用 Efraimidis–Spirakis 演算法：每題算 key = u^(1/w)（u 為 0–1 亂數），取 key 最大的 count 題。
     */
    fun pickByWeakness(pool: List<Question>, stats: List<QuestionAttemptStat>, count: Int): List<Question> {
        val weights = WeaknessWeights(stats)
        return pool
            .map { q -> q to random.nextDouble().pow(1.0 / weights.of(q)) }
            .sortedByDescending { it.second }
            .take(count.coerceAtLeast(0))
            .map { it.first }
    }
}

/**
 * 每題權重 = 基本分 + 領域弱度 + 單題錯誤率
 *   領域弱度   = 1 − 領域正確率
 *   單題錯誤率 = 這題答錯的比例
 * 兩個比例都用「拉普拉斯平滑」：(答對 + 1) / (作答 + 2)。
 * 沒做過的題目或領域會被當成 50%，不會因為樣本少而極端。
 */
class WeaknessWeights(stats: List<QuestionAttemptStat>) {
    private val byQuestion = stats.associateBy { it.questionId }
    private val byDomain: Map<Domain, Pair<Int, Int>> = stats
        .groupBy { Domain.fromCode(it.domainCode) }
        .mapValues { (_, rows) -> rows.sumOf { it.correct } to rows.sumOf { it.total } }

    fun of(q: Question): Double {
        val (domainCorrect, domainTotal) = byDomain[q.domain] ?: (0 to 0)
        val domainAccuracy = smoothed(domainCorrect, domainTotal)
        val stat = byQuestion[q.id]
        val questionWrongRate = 1 - smoothed(stat?.correct ?: 0, stat?.total ?: 0)
        return BASE + (1 - domainAccuracy) + questionWrongRate
    }

    private fun smoothed(correct: Int, total: Int) = (correct + 1.0) / (total + 2.0)

    companion object {
        /** 讓再熟的題目也有被抽到的機會 */
        const val BASE = 0.1
    }
}
