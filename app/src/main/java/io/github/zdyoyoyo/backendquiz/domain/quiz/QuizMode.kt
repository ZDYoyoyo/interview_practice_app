package io.github.zdyoyoyo.backendquiz.domain.quiz

enum class QuizMode(val code: String, val title: String) {
    RANDOM("random", "做題"),
    WEAKNESS("weakness", "弱點練習"),
    REVIEW("review", "今日複習"),
    MISTAKES("mistakes", "錯題重做");

    companion object {
        fun fromCode(code: String?): QuizMode = entries.firstOrNull { it.code == code } ?: RANDOM
    }
}
