package io.github.zdyoyoyo.backendquiz.domain.quiz

import io.github.zdyoyoyo.backendquiz.domain.model.Question

object AnswerChecker {
    /** 單選、多選都一樣：選的集合必須和答案集合完全相同（多選題全對才算對）。 */
    fun isCorrect(question: Question, selected: Set<String>): Boolean =
        selected.isNotEmpty() && selected == question.answer
}
