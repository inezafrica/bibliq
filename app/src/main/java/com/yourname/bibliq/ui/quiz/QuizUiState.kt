package com.yourname.bibliq.ui.quiz

import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.domain.PowerUpType

sealed interface AnswerState {
    data object Waiting : AnswerState
    data class Correct(val chosen: Int) : AnswerState
    data class Wrong(val chosen: Int?, val timedOut: Boolean) : AnswerState
}

enum class SegmentState { DONE_CORRECT, DONE_WRONG, CURRENT, UPCOMING }

data class QuizUiState(
    val levelNumber: Int,
    val question: LocalizedQuestion,
    val questionIndex: Int,
    val totalQuestions: Int,
    val progress: List<SegmentState>,
    val chancesLeft: Int,
    val secondsLeft: Int,
    val timeFraction: Float,
    val score: Int,
    val lastGain: Int?,
    val hiddenOptions: Set<Int>,
    val verseRevealed: Boolean,
    val answer: AnswerState,
    val talents: Int,
    val encouragement: String?,
    // Not in the original spec table, but needed to enforce "once per question" -- extra
    // time has no other state signal the UI can use to grey its button out.
    val usedPowerUps: Set<PowerUpType> = emptySet()
)
