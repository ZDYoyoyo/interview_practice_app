package io.github.zdyoyoyo.backendquiz.domain.quiz

import io.github.zdyoyoyo.backendquiz.domain.model.Question
import kotlin.random.Random

/**
 * 出題策略。Random 從建構子注入，測試時可以傳固定種子，讓結果可重現。
 * （第二階段會在這裡加上「依弱點加權」）
 */
class QuestionPicker(private val random: Random = Random.Default) {

    fun pickRandom(pool: List<Question>, count: Int): List<Question> =
        pool.shuffled(random).take(count.coerceAtLeast(0))
}
