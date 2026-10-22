package com.yourname.bibliq.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.bibliq.R
import com.yourname.bibliq.data.model.Level
import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.data.repository.ContentRepository
import com.yourname.bibliq.data.repository.ProgressRepository
import com.yourname.bibliq.domain.LevelResult
import com.yourname.bibliq.domain.PowerUpType
import com.yourname.bibliq.domain.QuizEngine
import com.yourname.bibliq.domain.TalentWallet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val content = ContentRepository.get(application)
    private val talentWallet: TalentWallet = ProgressRepository.get(application)

    private val _uiState = MutableStateFlow<QuizUiState?>(null)
    val uiState: StateFlow<QuizUiState?> = _uiState.asStateFlow()

    private lateinit var level: Level
    private lateinit var questions: List<LocalizedQuestion>
    private lateinit var segments: MutableList<SegmentState>
    private var levelNumber = 0
    private var currentIndex = 0
    private var chancesLeft = QuizEngine.CHANCES_PER_LEVEL
    private var score = 0
    private var correctCount = 0
    private var secondsLeft = QuizEngine.QUESTION_TIME_SECONDS
    private var hiddenOptions: Set<Int> = emptySet()
    private var verseRevealed = false
    private var answerState: AnswerState = AnswerState.Waiting
    private var lastGain: Int? = null
    private var encouragement: String? = null
    private val usedPowerUps = mutableSetOf<PowerUpType>()
    private var timerJob: Job? = null
    private var isPaused = false

    private var onLevelFinished: ((LevelResult) -> Unit)? = null
    private var started = false

    fun start(levelNumber: Int, language: String, onLevelFinished: (LevelResult) -> Unit) {
        if (started && this.levelNumber == levelNumber) return
        started = true
        this.onLevelFinished = onLevelFinished
        this.levelNumber = levelNumber

        val game = content.content.value ?: error("Content is not loaded")
        level = game.level(levelNumber) ?: error("Level $levelNumber not found")
        questions = game.questionsFor(level, language)
        currentIndex = 0
        chancesLeft = QuizEngine.CHANCES_PER_LEVEL
        score = 0
        correctCount = 0
        segments = MutableList(questions.size) { index ->
            if (index == 0) SegmentState.CURRENT else SegmentState.UPCOMING
        }
        beginQuestion()
    }

    fun onAnswer(index: Int) {
        if (answerState !is AnswerState.Waiting) return
        timerJob?.cancel()
        val correct = index == questions[currentIndex].correctIndex
        applyAnswerResult(chosenIndex = index, correct = correct, timedOut = false)
    }

    fun onPowerUp(type: PowerUpType) {
        if (answerState !is AnswerState.Waiting) return
        if (type in usedPowerUps) return
        val question = questions[currentIndex]
        if (type == PowerUpType.FIFTY_FIFTY && !QuizEngine.is5050Allowed(question.type)) return
        if (!talentWallet.trySpend(type.talentCost)) return

        usedPowerUps += type
        when (type) {
            PowerUpType.FIFTY_FIFTY -> {
                val wrongIndices = question.options.indices.filter { it != question.correctIndex }
                hiddenOptions = wrongIndices.shuffled().take(2).toSet()
            }
            PowerUpType.EXTRA_TIME -> {
                secondsLeft = QuizEngine.applyExtraTime(secondsLeft)
            }
            PowerUpType.SKIP -> {
                timerJob?.cancel()
                val gain = QuizEngine.scoreForSkip()
                score += gain
                correctCount++
                segments[currentIndex] = SegmentState.DONE_CORRECT
                lastGain = gain
                answerState = AnswerState.Correct(question.correctIndex)
            }
            PowerUpType.REVEAL_VERSE -> {
                verseRevealed = true
            }
        }
        emitState()
    }

    fun onNext() {
        if (QuizEngine.isFailedByChances(chancesLeft)) {
            finishLevel()
            return
        }
        val nextIndex = currentIndex + 1
        if (nextIndex >= questions.size) {
            finishLevel()
            return
        }
        currentIndex = nextIndex
        segments[currentIndex] = SegmentState.CURRENT
        beginQuestion()
    }

    fun onQuit() {
        timerJob?.cancel()
    }

    fun onAppPaused() {
        isPaused = true
    }

    fun onAppResumed() {
        isPaused = false
    }

    private fun beginQuestion() {
        hiddenOptions = emptySet()
        verseRevealed = false
        answerState = AnswerState.Waiting
        lastGain = null
        encouragement = null
        usedPowerUps.clear()
        secondsLeft = QuizEngine.QUESTION_TIME_SECONDS
        emitState()
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (secondsLeft > 0) {
                delay(1_000)
                if (isPaused) continue
                if (answerState !is AnswerState.Waiting) return@launch
                secondsLeft -= 1
                emitState()
            }
            if (answerState is AnswerState.Waiting) {
                applyAnswerResult(chosenIndex = null, correct = false, timedOut = true)
            }
        }
    }

    private fun applyAnswerResult(chosenIndex: Int?, correct: Boolean, timedOut: Boolean) {
        val gain = if (correct) QuizEngine.scoreForCorrectAnswer(secondsLeft) else QuizEngine.scoreForWrongOrTimeout()
        score += gain
        if (correct) {
            correctCount++
        } else {
            chancesLeft = QuizEngine.chancesAfterWrongAnswer(chancesLeft)
        }
        segments[currentIndex] = if (correct) SegmentState.DONE_CORRECT else SegmentState.DONE_WRONG
        answerState = if (correct) {
            AnswerState.Correct(chosenIndex!!)
        } else {
            AnswerState.Wrong(chosenIndex, timedOut)
        }
        lastGain = if (correct) gain else null
        val context = getApplication<Application>()
        encouragement = when {
            correct -> null
            timedOut -> context.getString(R.string.times_up)
            else -> context.getString(ENCOURAGEMENT_RES.random())
        }
        emitState()
    }

    private fun finishLevel() {
        timerJob?.cancel()
        val bonus = QuizEngine.perfectLevelBonus(correctCount, questions.size)
        val finalScore = score + bonus
        val stars = QuizEngine.starsFor(correctCount, level.starThresholds)
        val failed = QuizEngine.isLevelFailed(chancesLeft, correctCount, level.starThresholds)
        onLevelFinished?.invoke(LevelResult(levelNumber, correctCount, finalScore, stars, failed))
    }

    private fun emitState() {
        _uiState.value = QuizUiState(
            levelNumber = levelNumber,
            question = questions[currentIndex],
            questionIndex = currentIndex,
            totalQuestions = questions.size,
            progress = segments.toList(),
            chancesLeft = chancesLeft,
            secondsLeft = secondsLeft,
            timeFraction = secondsLeft.toFloat() / QuizEngine.QUESTION_TIME_SECONDS,
            score = score,
            lastGain = lastGain,
            hiddenOptions = hiddenOptions,
            verseRevealed = verseRevealed,
            answer = answerState,
            talents = talentWallet.balance.value,
            encouragement = encouragement,
            usedPowerUps = usedPowerUps.toSet()
        )
    }

    private companion object {
        val ENCOURAGEMENT_RES = listOf(
            R.string.encourage_1,
            R.string.encourage_2,
            R.string.encourage_3
        )
    }
}
