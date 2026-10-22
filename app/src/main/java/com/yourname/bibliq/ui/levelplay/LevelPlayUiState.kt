package com.yourname.bibliq.ui.levelplay

import com.yourname.bibliq.data.model.LocalizedQuestion

sealed interface LevelPlayUiState {

    data object Loading : LevelPlayUiState

    data class Error(val message: String) : LevelPlayUiState

    data class Playing(
        val levelNumber: Int,
        val questionIndex: Int,
        val totalQuestions: Int,
        val question: LocalizedQuestion,
        val selectedIndex: Int?,
        val isAnswerRevealed: Boolean,
        val correctCount: Int
    ) : LevelPlayUiState

    data class Finished(
        val levelNumber: Int,
        val correctCount: Int,
        val totalQuestions: Int,
        val stars: Int
    ) : LevelPlayUiState
}
