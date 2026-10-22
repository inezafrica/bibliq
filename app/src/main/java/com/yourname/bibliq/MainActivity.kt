package com.yourname.bibliq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.bibliq.data.model.FALLBACK_LANGUAGE
import com.yourname.bibliq.data.model.SUPPORTED_LANGUAGES
import com.yourname.bibliq.data.repository.ContentRepository
import com.yourname.bibliq.data.repository.ProgressRepository
import com.yourname.bibliq.data.repository.RecordedResult
import com.yourname.bibliq.domain.LevelResult
import com.yourname.bibliq.ui.home.HomeScreen
import com.yourname.bibliq.ui.quiz.QuizScreen
import com.yourname.bibliq.ui.result.LevelResultScreen
import com.yourname.bibliq.ui.theme.BibliQTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = ContentRepository.get(this)
        val progress = ProgressRepository.get(this)
        setContent {
            BibliQTheme {
                BibliQApp(content = content, progress = progress)
            }
        }
    }
}

private data class FinishedLevel(val result: LevelResult, val totalQuestions: Int, val recorded: RecordedResult)

@Composable
private fun BibliQApp(content: ContentRepository, progress: ProgressRepository) {
    val game by content.content.collectAsState()
    var quizLevel by rememberSaveable { mutableStateOf<Int?>(null) }
    // A fresh key per attempt gives each attempt its own QuizViewModel, while
    // rotation keeps the same one.
    var quizSession by rememberSaveable { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf<FinishedLevel?>(null) }
    val language = remember { appLanguage() }

    fun startLevel(number: Int) {
        finished = null
        if (progress.livesStatus().lives == 0) {
            quizLevel = null
            return
        }
        quizSession += 1
        quizLevel = number
    }

    val loaded = game
    val level = quizLevel
    val done = finished

    when {
        done != null && loaded != null -> {
            val nextLevel = loaded.levels.map { it.number }.filter { it > done.result.levelNumber }.minOrNull()
            LevelResultScreen(
                result = done.result,
                totalQuestions = done.totalQuestions,
                talentsEarned = done.recorded.talentsEarned,
                lifeLost = done.recorded.lifeLost,
                hasNextLevel = nextLevel != null,
                canPlay = progress.livesStatus().lives > 0,
                onNextLevel = { nextLevel?.let(::startLevel) },
                onRetry = { startLevel(done.result.levelNumber) },
                onBackToMap = { finished = null }
            )
        }

        level != null && loaded != null -> QuizScreen(
            levelNumber = level,
            language = language,
            onLevelFinished = { result ->
                val total = loaded.level(result.levelNumber)?.questionIds?.size ?: 0
                finished = FinishedLevel(result, total, progress.recordResult(result))
                quizLevel = null
            },
            onQuit = { quizLevel = null },
            viewModel = viewModel(key = "quiz-$quizSession")
        )

        else -> HomeScreen(onPlayLevel = ::startLevel)
    }
}

private fun appLanguage(): String =
    Locale.getDefault().language.takeIf { it in SUPPORTED_LANGUAGES } ?: FALLBACK_LANGUAGE
