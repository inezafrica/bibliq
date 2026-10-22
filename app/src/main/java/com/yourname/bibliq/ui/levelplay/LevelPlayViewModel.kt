package com.yourname.bibliq.ui.levelplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yourname.bibliq.data.model.FALLBACK_LANGUAGE
import com.yourname.bibliq.data.model.Level
import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.data.model.SUPPORTED_LANGUAGES
import com.yourname.bibliq.data.repository.LevelRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LevelPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LevelRepository(application)

    private val language: String =
        SUPPORTED_LANGUAGES.firstOrNull { it == Locale.getDefault().language } ?: FALLBACK_LANGUAGE

    private val _uiState = MutableStateFlow<LevelPlayUiState>(LevelPlayUiState.Loading)
    val uiState: StateFlow<LevelPlayUiState> = _uiState.asStateFlow()

    private var level: Level? = null
    private var questions: List<LocalizedQuestion> = emptyList()

    fun startLevel(levelNumber: Int) {
        _uiState.value = LevelPlayUiState.Loading
        try {
            val pack = repository.loadPack()
            val loadedLevel = pack.levels.find { it.number == levelNumber }
                ?: throw IllegalArgumentException("Level $levelNumber not found")

            level = loadedLevel
            questions = repository.getQuestionsForLevel(levelNumber, language)
            _uiState.value = playingState(questionIndex = 0, correctCount = 0)
        } catch (e: Exception) {
            _uiState.value = LevelPlayUiState.Error(e.message ?: "Failed to load level $levelNumber")
        }
    }

    fun selectAnswer(index: Int) {
        val current = _uiState.value as? LevelPlayUiState.Playing ?: return
        if (current.isAnswerRevealed) return
        _uiState.value = current.copy(selectedIndex = index, isAnswerRevealed = true)
    }

    fun nextQuestion() {
        val current = _uiState.value as? LevelPlayUiState.Playing ?: return
        if (!current.isAnswerRevealed) return

        val wasCorrect = current.selectedIndex == current.question.correctIndex
        val correctCount = current.correctCount + if (wasCorrect) 1 else 0
        val nextIndex = current.questionIndex + 1

        _uiState.value = if (nextIndex >= current.totalQuestions) {
            val loadedLevel = level ?: return
            LevelPlayUiState.Finished(
                levelNumber = current.levelNumber,
                correctCount = correctCount,
                totalQuestions = current.totalQuestions,
                stars = starsFor(correctCount, loadedLevel.starThresholds)
            )
        } else {
            playingState(questionIndex = nextIndex, correctCount = correctCount)
        }
    }

    private fun playingState(questionIndex: Int, correctCount: Int): LevelPlayUiState.Playing {
        val loadedLevel = level ?: error("Level not loaded")
        return LevelPlayUiState.Playing(
            levelNumber = loadedLevel.number,
            questionIndex = questionIndex,
            totalQuestions = questions.size,
            question = questions[questionIndex],
            selectedIndex = null,
            isAnswerRevealed = false,
            correctCount = correctCount
        )
    }

    private fun starsFor(correctCount: Int, thresholds: List<Int>): Int = when {
        correctCount >= thresholds[2] -> 3
        correctCount >= thresholds[1] -> 2
        correctCount >= thresholds[0] -> 1
        else -> 0
    }
}
