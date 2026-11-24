package io.github.zdyoyoyo.backendquiz.domain.stats

import io.github.zdyoyoyo.backendquiz.domain.model.Domain

/** 單題的作答彙總（由資料庫 GROUP BY 算出） */
data class QuestionAttemptStat(
    val questionId: String,
    val domainCode: String,
    val total: Int,
    val correct: Int,
)

data class DomainStat(
    val domain: Domain,
    val total: Int,
    val correct: Int,
    /** 這個領域答過幾道不同的題目 */
    val questionsAttempted: Int,
) {
    val accuracy: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
}

data class StatsSummary(
    /** 有作答紀錄的領域，依 Domain 定義順序 */
    val domains: List<DomainStat>,
    /** 正確率最低的領域（最多 3 個；只算作答次數足夠、且不是全對的） */
    val weakest: List<DomainStat>,
    val totalAttempts: Int,
    val totalCorrect: Int,
) {
    val overallAccuracy: Double get() = if (totalAttempts == 0) 0.0 else totalCorrect.toDouble() / totalAttempts
}

object StatsCalculator {
    /** 作答次數太少時正確率沒有意義（答 1 錯 1 = 0%），不列入最弱排名 */
    const val MIN_ATTEMPTS_FOR_RANKING = 5
    const val WEAKEST_COUNT = 3

    fun summarize(stats: List<QuestionAttemptStat>): StatsSummary {
        // groupBy ≈ Collectors.groupingBy；未知代碼會被 fromCode 歸到 OTHER
        val domains = stats.groupBy { Domain.fromCode(it.domainCode) }
            .map { (domain, rows) ->
                DomainStat(domain, rows.sumOf { it.total }, rows.sumOf { it.correct }, rows.size)
            }
            .sortedBy { it.domain.ordinal }

        val weakest = domains
            // 作答次數夠多，且有答錯過（全對的領域不算弱點）
            .filter { it.total >= MIN_ATTEMPTS_FOR_RANKING && it.correct < it.total }
            // 正確率低的在前；一樣的話，答得多的在前（樣本較可信）
            .sortedWith(compareBy<DomainStat> { it.accuracy }.thenByDescending { it.total })
            .take(WEAKEST_COUNT)

        return StatsSummary(domains, weakest, stats.sumOf { it.total }, stats.sumOf { it.correct })
    }
}
