package com.yourname.bibliq.ui.levelplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yourname.bibliq.ui.theme.BibliqFeedbackColors

@Composable
fun LevelPlayScreen(
    levelNumber: Int,
    modifier: Modifier = Modifier,
    viewModel: LevelPlayViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(levelNumber) {
        viewModel.startLevel(levelNumber)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is LevelPlayUiState.Loading -> LoadingContent()
            is LevelPlayUiState.Error -> ErrorContent(state.message)
            is LevelPlayUiState.Playing -> PlayingContent(
                state = state,
                onAnswerSelected = viewModel::selectAnswer,
                onNext = viewModel::nextQuestion
            )
            is LevelPlayUiState.Finished -> FinishedContent(
                state = state,
                onPlayAgain = { viewModel.startLevel(state.levelNumber) }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun PlayingContent(
    state: LevelPlayUiState.Playing,
    onAnswerSelected: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = "Question ${state.questionIndex + 1} / ${state.totalQuestions}",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.questionIndex + 1).toFloat() / state.totalQuestions },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = state.question.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.question.options.size) { index ->
                AnswerOption(
                    label = state.question.options[index],
                    isSelected = state.selectedIndex == index,
                    isCorrectAnswer = index == state.question.correctIndex,
                    isRevealed = state.isAnswerRevealed,
                    onClick = { onAnswerSelected(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isAnswerRevealed) {
            key(state.questionIndex) {
                AnswerFeedback(
                    isCorrect = state.selectedIndex == state.question.correctIndex,
                    reference = state.question.reference,
                    onNext = onNext
                )
            }
        }
    }
}

@Composable
private fun AnswerOption(
    label: String,
    isSelected: Boolean,
    isCorrectAnswer: Boolean,
    isRevealed: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !isRevealed -> MaterialTheme.colorScheme.surfaceVariant
        isCorrectAnswer -> BibliqFeedbackColors.Correct
        isSelected -> BibliqFeedbackColors.Incorrect
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !isRevealed -> MaterialTheme.colorScheme.onSurfaceVariant
        isCorrectAnswer || isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = onClick,
        enabled = !isRevealed,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        )
    ) {
        Text(text = label, color = contentColor)
    }
}

@Composable
private fun AnswerFeedback(
    isCorrect: Boolean,
    reference: String,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = if (isCorrect) "Correct!" else "Not quite.",
            style = MaterialTheme.typography.titleMedium,
            color = if (isCorrect) BibliqFeedbackColors.Correct else BibliqFeedbackColors.Incorrect
        )
        Text(text = reference, style = MaterialTheme.typography.bodyMedium)

        if (isCorrect) {
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/oli-correct.json"))
            val progress by animateLottieCompositionAsState(composition, iterations = 1)
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }
    }
}

@Composable
private fun FinishedContent(
    state: LevelPlayUiState.Finished,
    onPlayAgain: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/star_pop.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(120.dp)
        )
        Text(text = "Level ${state.levelNumber} complete!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = starsLabel(state.stars), style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${state.correctCount} / ${state.totalQuestions} correct")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPlayAgain) {
            Text("Play again")
        }
    }
}

private fun starsLabel(stars: Int): String = "★".repeat(stars) + "☆".repeat(3 - stars)
