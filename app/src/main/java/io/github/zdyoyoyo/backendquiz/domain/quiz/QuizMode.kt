package io.github.zdyoyoyo.backendquiz.domain.quiz

enum class QuizMode(val code: String) {
    RANDOM("random"),
    MISTAKES("mistakes");

    companion object {
        fun fromCode(code: String?): QuizMode = entries.firstOrNull { it.code == code } ?: RANDOM
    }
}
