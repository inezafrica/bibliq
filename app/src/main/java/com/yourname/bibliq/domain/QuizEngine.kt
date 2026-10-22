package com.yourname.bibliq.domain

import com.yourname.bibliq.data.model.QuestionType

enum class PowerUpType(val talentCost: Int) {
    FIFTY_FIFTY(30),
    EXTRA_TIME(20),
    SKIP(50),
    REVEAL_VERSE(25)
}

object QuizEngine {
    const val CHANCES_PER_LEVEL = 3
    const val QUESTION_TIME_SECONDS = 20
    const val EXTRA_TIME_SECONDS = 10
    const val BASE_CORRECT_POINTS = 10
    const val MAX_SPEED_BONUS = 20
    const val PERFECT_LEVEL_BONUS = 50

    /** 1 point per second left, capped at [MAX_SPEED_BONUS]. */
    fun speedBonus(secondsLeft: Int): Int = secondsLeft.coerceIn(0, MAX_SPEED_BONUS)

    fun scoreForCorrectAnswer(secondsLeft: Int): Int = BASE_CORRECT_POINTS + speedBonus(secondsLeft)

    /** Skip counts as correct but earns no speed bonus. */
    fun scoreForSkip(): Int = BASE_CORRECT_POINTS

    fun scoreForWrongOrTimeout(): Int = 0

    fun perfectLevelBonus(correctCount: Int, totalQuestions: Int): Int =
        if (totalQuestions > 0 && correctCount == totalQuestions) PERFECT_LEVEL_BONUS else 0

    fun chancesAfterWrongAnswer(chancesLeft: Int): Int = (chancesLeft - 1).coerceAtLeast(0)

    fun isFailedByChances(chancesLeft: Int): Boolean = chancesLeft <= 0

    /** Below the first threshold -> 0 (failed); otherwise 1-3 stars. */
    fun starsFor(correctCount: Int, starThresholds: List<Int>): Int {
        require(starThresholds.size == 3) { "starThresholds must have exactly 3 values" }
        return when {
            correctCount >= starThresholds[2] -> 3
            correctCount >= starThresholds[1] -> 2
            correctCount >= starThresholds[0] -> 1
            else -> 0
        }
    }

    fun isLevelFailed(chancesLeft: Int, correctCount: Int, starThresholds: List<Int>): Boolean =
        isFailedByChances(chancesLeft) || starsFor(correctCount, starThresholds) == 0

    /** 50/50 never applies to TRUE_FALSE (only 2 options to begin with). */
    fun is5050Allowed(type: QuestionType): Boolean = type != QuestionType.TRUE_FALSE

    fun applyExtraTime(secondsLeft: Int): Int = secondsLeft + EXTRA_TIME_SECONDS
}

data class LevelResult(
    val levelNumber: Int,
    val correctCount: Int,
    val score: Int,
    val stars: Int,
    val failed: Boolean
)
