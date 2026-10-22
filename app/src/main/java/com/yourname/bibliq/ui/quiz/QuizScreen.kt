package com.yourname.bibliq.ui.quiz

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.bibliq.R
import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.data.model.QuestionType
import com.yourname.bibliq.domain.LevelResult
import com.yourname.bibliq.domain.PowerUpType
import com.yourname.bibliq.domain.QuizEngine
import com.yourname.bibliq.ui.theme.Baloo2
import com.yourname.bibliq.ui.theme.BibliQColors
import com.yourname.bibliq.ui.theme.BibliQTheme
import com.yourname.bibliq.ui.theme.LoraMediumItalic
import com.yourname.bibliq.ui.theme.Nunito
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun QuizScreen(
    levelNumber: Int,
    language: String,
    onLevelFinished: (LevelResult) -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel()
) {
    LaunchedEffect(levelNumber) {
        viewModel.start(levelNumber, language, onLevelFinished)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.uiState.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            QuizScreenContent(
                state = current,
                onAnswer = viewModel::onAnswer,
                onPowerUp = viewModel::onPowerUp,
                onNext = viewModel::onNext,
                onQuitRequested = {
                    viewModel.onQuit()
                    onQuit()
                }
            )
        }
    }
}

@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

private fun <T> motionSpec(enabled: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> =
    if (enabled) spec else snap()

@Composable
private fun QuizScreenContent(
    state: QuizUiState,
    onAnswer: (Int) -> Unit,
    onPowerUp: (PowerUpType) -> Unit,
    onNext: () -> Unit,
    onQuitRequested: () -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var showQuitDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = !showQuitDialog) { showQuitDialog = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BibliQColors.ScreenSky)
    ) {
        BackgroundHills()

        Column(Modifier.fillMaxSize()) {
            QuizTopBar(
                progress = state.progress,
                chancesLeft = state.chancesLeft,
                animationsEnabled = animationsEnabled,
                onQuitClick = { showQuitDialog = true }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                TimerRow(
                    secondsLeft = state.secondsLeft,
                    timeFraction = state.timeFraction,
                    score = state.score,
                    lastGain = state.lastGain
                )
                Spacer(Modifier.height(14.dp))

                if (state.question.type != QuestionType.IMAGE) {
                    TypeChip(type = state.question.type)
                    Spacer(Modifier.height(14.dp))
                }

                if (state.verseRevealed && state.answer is AnswerState.Waiting) {
                    VerseChip(reference = state.question.reference)
                    Spacer(Modifier.height(14.dp))
                }

                when (state.question.type) {
                    QuestionType.TRUE_FALSE -> TrueFalseBody(
                        question = state.question,
                        answer = state.answer,
                        onAnswerSelected = onAnswer
                    )
                    QuestionType.IMAGE -> PictureBody(
                        question = state.question,
                        hiddenOptions = state.hiddenOptions,
                        answer = state.answer,
                        onAnswerSelected = onAnswer
                    )
                    else -> McqBody(
                        question = state.question,
                        hiddenOptions = state.hiddenOptions,
                        answer = state.answer,
                        onAnswerSelected = onAnswer
                    )
                }
                Spacer(Modifier.height(96.dp))
            }

            PowerUpBar(
                talents = state.talents,
                questionType = state.question.type,
                usedPowerUps = state.usedPowerUps,
                answerState = state.answer,
                onPowerUp = onPowerUp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        FeedbackPanel(
            answer = state.answer,
            question = state.question,
            lastGain = state.lastGain,
            encouragement = state.encouragement,
            animationsEnabled = animationsEnabled,
            onNext = onNext,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showQuitDialog) {
        QuitDialog(
            onConfirm = {
                showQuitDialog = false
                onQuitRequested()
            },
            onDismiss = { showQuitDialog = false }
        )
    }
}

@Composable
private fun BackgroundHills() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(BibliQColors.HillDark)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 110.dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(BibliQColors.HillLight)
        )
    }
}

// ---------------------------------------------------------------------------
// Top bar: quit, progress, chances
// ---------------------------------------------------------------------------

@Composable
private fun QuizTopBar(
    progress: List<SegmentState>,
    chancesLeft: Int,
    animationsEnabled: Boolean,
    onQuitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 20.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuitButton(onClick = onQuitClick)
        ProgressSegments(progress = progress, modifier = Modifier.weight(1f))
        ChancesRow(chancesLeft = chancesLeft, animationsEnabled = animationsEnabled)
    }
}

@Composable
private fun QuitButton(onClick: () -> Unit) {
    val desc = stringResource(R.string.quit_title)
    Box(Modifier.size(44.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .clip(CircleShape)
                .background(Color(0xFFB9D3EE))
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
                .semantics(desc),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = BibliQColors.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProgressSegments(progress: List<SegmentState>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        progress.forEach { segment ->
            val color = when (segment) {
                SegmentState.DONE_CORRECT -> BibliQColors.OliveGreen
                SegmentState.DONE_WRONG -> BibliQColors.Coral
                SegmentState.CURRENT -> BibliQColors.RoyalBlue
                SegmentState.UPCOMING -> Color.White
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ChancesRow(chancesLeft: Int, animationsEnabled: Boolean, modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.chances_left, chancesLeft)
    Row(
        modifier = modifier.semantics(desc),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(QuizEngine.CHANCES_PER_LEVEL) { i ->
            val lost = i >= chancesLeft
            val offsetY by animateDpAsState(
                if (lost) 4.dp else 0.dp,
                animationSpec = motionSpec(animationsEnabled, tween(300)),
                label = "leafDrop"
            )
            val rotation by animateFloatAsState(
                if (lost) 18f else 0f,
                animationSpec = motionSpec(animationsEnabled, tween(300)),
                label = "leafRotate"
            )
            Image(
                painter = painterResource(if (lost) R.drawable.ic_leaf_lost else R.drawable.ic_leaf_chance),
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .offset(y = offsetY)
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Timer row
// ---------------------------------------------------------------------------

@Composable
private fun TimerRow(
    secondsLeft: Int,
    timeFraction: Float,
    score: Int,
    lastGain: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimerPill(secondsLeft = secondsLeft)
            ScoreDisplay(score = score, lastGain = lastGain)
        }
        Spacer(Modifier.height(8.dp))
        TimeBar(timeFraction = timeFraction)
    }
}

@Composable
private fun TimerPill(secondsLeft: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 12.dp)
    ) {
        Image(painterResource(R.drawable.ic_timer_sundial), null, Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            formatMmSs(secondsLeft),
            fontFamily = Baloo2,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = BibliQColors.RoyalBlue
        )
    }
}

@Composable
private fun TimeBar(timeFraction: Float, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = timeFraction.coerceIn(0f, 1f),
        animationSpec = tween(1_000, easing = LinearEasing),
        label = "timeBar"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .clip(RoundedCornerShape(4.dp))
                .background(BibliQColors.Gold)
        )
    }
}

@Composable
private fun ScoreDisplay(score: Int, lastGain: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_star_filled), null, Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            score.toString(),
            fontFamily = Baloo2,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = BibliQColors.Ink
        )
        AnimatedVisibility(visible = lastGain != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BibliQColors.OliveGreen)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "+${lastGain ?: 0}",
                        color = Color.White,
                        fontFamily = Baloo2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(clamped / 60, clamped % 60)
}

// ---------------------------------------------------------------------------
// Type chip
// ---------------------------------------------------------------------------

@Composable
private fun TypeChip(type: QuestionType, modifier: Modifier = Modifier) {
    val label = when (type) {
        QuestionType.TRUE_FALSE -> stringResource(R.string.type_true_false)
        QuestionType.IMAGE -> stringResource(R.string.type_picture)
        QuestionType.WHO_SAID -> stringResource(R.string.type_who_said)
        else -> stringResource(R.string.type_mcq)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BibliQColors.RoyalBlue)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Question card (shared shell for MCQ / TRUE_FALSE / IMAGE question text)
// ---------------------------------------------------------------------------

@Composable
private fun QuestionCard(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    textAlign: TextAlign = TextAlign.Start,
    centerVertically: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .drawBehind {
                drawRoundRect(
                    color = BibliQColors.ParchmentEdge,
                    topLeft = Offset(0f, 6.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            }
            .border(2.dp, BibliQColors.ParchmentEdge, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BibliQColors.Parchment)
            .padding(22.dp),
        contentAlignment = if (centerVertically) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text,
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            color = BibliQColors.Ink,
            textAlign = textAlign
        )
    }
}

// ---------------------------------------------------------------------------
// MCQ
// ---------------------------------------------------------------------------

@Composable
private fun McqBody(
    question: LocalizedQuestion,
    hiddenOptions: Set<Int>,
    answer: AnswerState,
    onAnswerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        QuestionCard(text = question.text, fontSize = 22.sp)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEachIndexed { index, optionText ->
                if (index !in hiddenOptions) {
                    AnswerButton(
                        label = optionText,
                        letter = ('A' + index).toString(),
                        index = index,
                        question = question,
                        answer = answer,
                        onClick = { onAnswerSelected(index) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Answer button (MCQ column + IMAGE grid)
// ---------------------------------------------------------------------------

@Composable
private fun AnswerButton(
    label: String,
    letter: String?,
    index: Int,
    question: LocalizedQuestion,
    answer: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 58.dp
) {
    val isCorrectOption = index == question.correctIndex
    val chosenIndex = chosenIndexOf(answer)
    val revealed = answer !is AnswerState.Waiting
    val isChosen = chosenIndex == index

    val containerColor = when {
        revealed && isCorrectOption -> BibliQColors.OliveGreen
        revealed && isChosen -> BibliQColors.Coral
        else -> Color.White
    }
    val edgeColor = when {
        revealed && isCorrectOption -> BibliQColors.OliveDark
        revealed && isChosen -> BibliQColors.CoralDark
        else -> BibliQColors.AnswerEdge
    }
    val textColor = if (revealed && isCorrectOption) Color.White else BibliQColors.Ink
    val dim = revealed && !isCorrectOption && !isChosen

    val scale by animateFloatAsState(if (revealed && isCorrectOption) 1.03f else 1f, label = "answerScale")
    var pressed by remember { mutableStateOf(false) }
    val pressOffset by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "answerPress")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight + 4.dp)
            .graphicsLayer(alpha = if (dim) 0.5f else 1f, scaleX = scale, scaleY = scale)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .drawBehind {
                    drawRoundRect(
                        color = edgeColor,
                        topLeft = Offset(0f, 4.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                }
                .offset(y = pressOffset)
                .border(2.dp, edgeColor, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(containerColor)
                .pointerInput(revealed) {
                    if (!revealed) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (letter != null) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(BibliQColors.LetterBadge),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            letter,
                            fontFamily = Baloo2,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = BibliQColors.RoyalBlue
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    label,
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor,
                    textAlign = if (letter == null) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (revealed && isCorrectOption) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                } else if (revealed && isChosen) {
                    Text("✕", color = BibliQColors.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

private fun chosenIndexOf(answer: AnswerState): Int? = when (answer) {
    is AnswerState.Correct -> answer.chosen
    is AnswerState.Wrong -> answer.chosen
    AnswerState.Waiting -> null
}

// ---------------------------------------------------------------------------
// TRUE / FALSE
// ---------------------------------------------------------------------------

private const val TRUE_INDEX = 0
private const val FALSE_INDEX = 1

@Composable
private fun TrueFalseBody(
    question: LocalizedQuestion,
    answer: AnswerState,
    onAnswerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        QuestionCard(
            text = question.text,
            fontSize = 24.sp,
            minHeight = 150.dp,
            textAlign = TextAlign.Center,
            centerVertically = true
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TrueFalseButton(
                isTrue = true,
                label = stringResource(R.string.answer_true),
                index = TRUE_INDEX,
                question = question,
                answer = answer,
                onClick = { onAnswerSelected(TRUE_INDEX) },
                modifier = Modifier.weight(1f)
            )
            TrueFalseButton(
                isTrue = false,
                label = stringResource(R.string.answer_false),
                index = FALSE_INDEX,
                question = question,
                answer = answer,
                onClick = { onAnswerSelected(FALSE_INDEX) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrueFalseButton(
    isTrue: Boolean,
    label: String,
    index: Int,
    question: LocalizedQuestion,
    answer: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCorrectOption = index == question.correctIndex
    val revealed = answer !is AnswerState.Waiting
    val isChosen = chosenIndexOf(answer) == index
    val circleColor = if (isTrue) BibliQColors.OliveGreen else BibliQColors.CoralDark
    val dim = revealed && !isCorrectOption && !isChosen
    var pressed by remember { mutableStateOf(false) }
    val pressOffset by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "tfPress")

    val edgeColor = when {
        revealed && isCorrectOption -> BibliQColors.OliveDark
        revealed && isChosen -> BibliQColors.CoralDark
        else -> BibliQColors.AnswerEdge
    }
    val faceColor = when {
        revealed && isCorrectOption -> BibliQColors.OliveGreen
        revealed && isChosen -> BibliQColors.Coral
        else -> Color.White
    }

    Box(modifier.height(174.dp).graphicsLayer(alpha = if (dim) 0.5f else 1f)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .drawBehind {
                    drawRoundRect(
                        color = edgeColor,
                        topLeft = Offset(0f, 4.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(24.dp))
                .background(faceColor)
                .pointerInput(revealed) {
                    if (!revealed) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isTrue) "✓" else "✕",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    label,
                    fontFamily = Baloo2,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = BibliQColors.Ink
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// IMAGE
// ---------------------------------------------------------------------------

@Composable
private fun PictureBody(
    question: LocalizedQuestion,
    hiddenOptions: Set<Int>,
    answer: AnswerState,
    onAnswerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 2f)) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFB9D3EE))
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(19.dp))
            ) {
                val imageUrl = question.imageUrl
                if (imageUrl != null) {
                    QuestionImage(imageUrl = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
            TypeChip(
                type = QuestionType.IMAGE,
                modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        QuestionCard(text = question.text, fontSize = 22.sp, textAlign = TextAlign.Center, centerVertically = true)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    rowOptions.forEachIndexed { colIndex, optionText ->
                        val index = rowIndex * 2 + colIndex
                        if (index !in hiddenOptions) {
                            AnswerButton(
                                label = optionText,
                                letter = null,
                                index = index,
                                question = question,
                                answer = answer,
                                onClick = { onAnswerSelected(index) },
                                modifier = Modifier.weight(1f),
                                minHeight = 68.dp
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Power-ups
// ---------------------------------------------------------------------------

@Composable
private fun PowerUpBar(
    talents: Int,
    questionType: QuestionType,
    usedPowerUps: Set<PowerUpType>,
    answerState: AnswerState,
    onPowerUp: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    val canInteract = answerState is AnswerState.Waiting
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PowerUpButton(
            iconRes = R.drawable.ic_power_staff,
            cost = PowerUpType.FIFTY_FIFTY.talentCost,
            contentDescription = stringResource(R.string.power_5050, PowerUpType.FIFTY_FIFTY.talentCost),
            enabled = canInteract &&
                talents >= PowerUpType.FIFTY_FIFTY.talentCost &&
                QuizEngine.is5050Allowed(questionType) &&
                PowerUpType.FIFTY_FIFTY !in usedPowerUps,
            onClick = { onPowerUp(PowerUpType.FIFTY_FIFTY) },
            modifier = Modifier.weight(1f)
        )
        PowerUpButton(
            iconRes = R.drawable.ic_power_sundial,
            cost = PowerUpType.EXTRA_TIME.talentCost,
            contentDescription = stringResource(R.string.power_time, PowerUpType.EXTRA_TIME.talentCost),
            enabled = canInteract &&
                talents >= PowerUpType.EXTRA_TIME.talentCost &&
                PowerUpType.EXTRA_TIME !in usedPowerUps,
            onClick = { onPowerUp(PowerUpType.EXTRA_TIME) },
            modifier = Modifier.weight(1f)
        )
        PowerUpButton(
            iconRes = R.drawable.ic_power_chariot,
            cost = PowerUpType.SKIP.talentCost,
            contentDescription = stringResource(R.string.power_skip, PowerUpType.SKIP.talentCost),
            enabled = canInteract &&
                talents >= PowerUpType.SKIP.talentCost &&
                PowerUpType.SKIP !in usedPowerUps,
            onClick = { onPowerUp(PowerUpType.SKIP) },
            modifier = Modifier.weight(1f)
        )
        PowerUpButton(
            iconRes = R.drawable.ic_power_scroll,
            cost = PowerUpType.REVEAL_VERSE.talentCost,
            contentDescription = stringResource(R.string.power_verse, PowerUpType.REVEAL_VERSE.talentCost),
            enabled = canInteract &&
                talents >= PowerUpType.REVEAL_VERSE.talentCost &&
                PowerUpType.REVEAL_VERSE !in usedPowerUps,
            onClick = { onPowerUp(PowerUpType.REVEAL_VERSE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PowerUpButton(
    iconRes: Int,
    cost: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val pressOffset by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp, label = "powerPress")

    Box(
        modifier = modifier
            .height(70.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics(contentDescription)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .drawBehind {
                    drawRoundRect(
                        color = BibliQColors.PowerEdge,
                        topLeft = Offset(0f, 4.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(iconRes), null, Modifier.size(42.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BibliQColors.Ink)
                .padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_talent), null, Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(cost.toString(), color = Color.White, fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Feedback panel
// ---------------------------------------------------------------------------

@Composable
private fun FeedbackPanel(
    answer: AnswerState,
    question: LocalizedQuestion,
    lastGain: Int?,
    encouragement: String?,
    animationsEnabled: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = answer !is AnswerState.Waiting,
        enter = if (animationsEnabled) {
            slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300))
        } else {
            fadeIn(snap())
        },
        exit = fadeOut(tween(if (animationsEnabled) 150 else 0)),
        modifier = modifier
    ) {
        when (val current = answer) {
            is AnswerState.Correct -> CorrectPanel(question = question, lastGain = lastGain, onNext = onNext)
            is AnswerState.Wrong -> WrongPanel(
                question = question,
                encouragement = encouragement,
                timedOut = current.timedOut,
                onNext = onNext
            )
            AnswerState.Waiting -> Unit
        }
    }
}

@Composable
private fun CorrectPanel(question: LocalizedQuestion, lastGain: Int?, onNext: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 264.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(BibliQColors.OliveGreen)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(BibliQColors.CorrectSheet)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/oli-correct.json"))
                val progress by animateLottieCompositionAsState(composition, iterations = 1)
                LottieAnimation(composition, { progress }, Modifier.size(96.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.well_done),
                        fontFamily = Baloo2,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = BibliQColors.OliveDark
                    )
                    val bonus = ((lastGain ?: QuizEngine.BASE_CORRECT_POINTS) - QuizEngine.BASE_CORRECT_POINTS)
                        .coerceAtLeast(0)
                    Text(
                        stringResource(R.string.points_gained, QuizEngine.BASE_CORRECT_POINTS, bonus),
                        fontFamily = Nunito,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = BibliQColors.Ink
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            VerseChip(reference = question.reference)
            Spacer(Modifier.height(18.dp))
            NextButton(
                label = stringResource(R.string.next),
                containerColor = BibliQColors.OliveGreen,
                edgeColor = BibliQColors.OliveDark,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun WrongPanel(
    question: LocalizedQuestion,
    encouragement: String?,
    timedOut: Boolean,
    onNext: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 278.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(BibliQColors.Coral)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(BibliQColors.WrongSheet)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/oli-sad.json"))
                val progress by animateLottieCompositionAsState(composition, iterations = 1)
                LottieAnimation(composition, { progress }, Modifier.size(96.dp))
                Spacer(Modifier.width(12.dp))
                SpeechBubble(
                    text = if (timedOut) stringResource(R.string.times_up) else encouragement.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            AnswerIsText(answerText = question.options.getOrElse(question.correctIndex) { "" })
            Spacer(Modifier.height(14.dp))
            VerseChip(reference = question.reference)
            Spacer(Modifier.height(18.dp))
            NextButton(
                label = stringResource(R.string.continue_button),
                containerColor = BibliQColors.RoyalBlue,
                edgeColor = BibliQColors.RoyalBlueDark,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(2.dp, BibliQColors.WrongBubbleEdge, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Text(text, fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BibliQColors.Ink)
    }
}

@Composable
private fun AnswerIsText(answerText: String, modifier: Modifier = Modifier) {
    val template = stringResource(R.string.answer_is)
    val parts = template.split("%s")
    val before = parts.getOrElse(0) { "" }
    val after = parts.getOrElse(1) { "" }
    val annotated = buildAnnotatedString {
        append(before)
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = BibliQColors.OliveDark)) {
            append(answerText)
        }
        append(after)
    }
    Text(annotated, modifier = modifier, fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BibliQColors.Ink)
}

@Composable
private fun VerseChip(reference: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BibliQColors.Parchment)
            .border(2.dp, BibliQColors.ParchmentEdge, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Image(painterResource(R.drawable.ic_verse_scroll), null, Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(reference, fontFamily = LoraMediumItalic, fontSize = 17.sp, color = BibliQColors.VerseText)
    }
}

@Composable
private fun NextButton(
    label: String,
    containerColor: Color,
    edgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val pressOffset by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "nextPress")
    Box(modifier.fillMaxWidth().height(63.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .drawBehind {
                    drawRoundRect(
                        color = edgeColor,
                        topLeft = Offset(0f, 5.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                }
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(18.dp))
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { onClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Color.White)
        }
    }
}

// ---------------------------------------------------------------------------
// Quit dialog
// ---------------------------------------------------------------------------

@Composable
private fun QuitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quit_title)) },
        text = { Text(stringResource(R.string.quit_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.quit_yes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.quit_no)) }
        }
    )
}

// ---------------------------------------------------------------------------
// contentDescription helper
// ---------------------------------------------------------------------------

private fun Modifier.semantics(description: String): Modifier = this.semantics {
    contentDescription = description
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private const val PREVIEW_DEVICE = "spec:width=411dp,height=891dp"

private fun previewQuestion(type: QuestionType, lang: String): LocalizedQuestion {
    val text = when (lang) {
        "fr" -> "Qui a construit l'arche ?"
        "es" -> "¿Quién construyó el arca?"
        else -> "Who built the ark?"
    }
    val mcqOptions = when (lang) {
        "fr" -> listOf("Noé", "Abraham", "Moïse", "David")
        "es" -> listOf("Noé", "Abraham", "Moisés", "David")
        else -> listOf("Noah", "Abraham", "Moses", "David")
    }
    val tfOptions = when (lang) {
        "fr" -> listOf("Vrai", "Faux")
        "es" -> listOf("Verdadero", "Falso")
        else -> listOf("True", "False")
    }
    val reference = when (lang) {
        "fr" -> "Genèse 6:14"
        "es" -> "Génesis 6:14"
        else -> "Genesis 6:14"
    }
    return LocalizedQuestion(
        id = "preview",
        type = type,
        difficulty = 1,
        text = text,
        options = if (type == QuestionType.TRUE_FALSE) tfOptions else mcqOptions,
        correctIndex = 0,
        imageUrl = if (type == QuestionType.IMAGE) "images/preview.webp" else null,
        reference = reference
    )
}

private fun previewState(
    type: QuestionType,
    lang: String,
    answer: AnswerState = AnswerState.Waiting
): QuizUiState {
    val encouragement = when (lang) {
        "fr" -> "Presque !"
        "es" -> "¡Casi!"
        else -> "Almost!"
    }
    return QuizUiState(
        levelNumber = 3,
        question = previewQuestion(type, lang),
        questionIndex = 3,
        totalQuestions = 10,
        progress = List(10) { i ->
            when {
                i < 3 -> SegmentState.DONE_CORRECT
                i == 3 -> SegmentState.CURRENT
                else -> SegmentState.UPCOMING
            }
        },
        chancesLeft = 2,
        secondsLeft = 14,
        timeFraction = 0.7f,
        score = 42,
        lastGain = if (answer is AnswerState.Correct) 18 else null,
        hiddenOptions = emptySet(),
        verseRevealed = false,
        answer = answer,
        talents = 240,
        encouragement = if (answer is AnswerState.Wrong) encouragement else null
    )
}

@Composable
private fun QuizPreview(state: QuizUiState) {
    BibliQTheme {
        QuizScreenContent(
            state = state,
            onAnswer = {},
            onPowerUp = {},
            onNext = {},
            onQuitRequested = {}
        )
    }
}

@Preview(name = "MCQ waiting - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewMcqWaitingEn() = QuizPreview(previewState(QuestionType.MCQ, "en"))

@Preview(name = "MCQ waiting - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewMcqWaitingFr() = QuizPreview(previewState(QuestionType.MCQ, "fr"))

@Preview(name = "MCQ waiting - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewMcqWaitingEs() = QuizPreview(previewState(QuestionType.MCQ, "es"))

@Preview(name = "TRUE_FALSE waiting - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTfWaitingEn() = QuizPreview(previewState(QuestionType.TRUE_FALSE, "en"))

@Preview(name = "TRUE_FALSE waiting - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTfWaitingFr() = QuizPreview(previewState(QuestionType.TRUE_FALSE, "fr"))

@Preview(name = "TRUE_FALSE waiting - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTfWaitingEs() = QuizPreview(previewState(QuestionType.TRUE_FALSE, "es"))

@Preview(name = "IMAGE waiting - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewImageWaitingEn() = QuizPreview(previewState(QuestionType.IMAGE, "en"))

@Preview(name = "IMAGE waiting - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewImageWaitingFr() = QuizPreview(previewState(QuestionType.IMAGE, "fr"))

@Preview(name = "IMAGE waiting - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewImageWaitingEs() = QuizPreview(previewState(QuestionType.IMAGE, "es"))

@Preview(name = "Correct - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewCorrectEn() = QuizPreview(previewState(QuestionType.MCQ, "en", AnswerState.Correct(0)))

@Preview(name = "Correct - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewCorrectFr() = QuizPreview(previewState(QuestionType.MCQ, "fr", AnswerState.Correct(0)))

@Preview(name = "Correct - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewCorrectEs() = QuizPreview(previewState(QuestionType.MCQ, "es", AnswerState.Correct(0)))

@Preview(name = "Wrong - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewWrongEn() = QuizPreview(previewState(QuestionType.MCQ, "en", AnswerState.Wrong(1, false)))

@Preview(name = "Wrong - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewWrongFr() = QuizPreview(previewState(QuestionType.MCQ, "fr", AnswerState.Wrong(1, false)))

@Preview(name = "Wrong - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewWrongEs() = QuizPreview(previewState(QuestionType.MCQ, "es", AnswerState.Wrong(1, false)))

@Preview(name = "Time's up - EN", locale = "en", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTimeUpEn() = QuizPreview(previewState(QuestionType.MCQ, "en", AnswerState.Wrong(null, true)))

@Preview(name = "Time's up - FR", locale = "fr", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTimeUpFr() = QuizPreview(previewState(QuestionType.MCQ, "fr", AnswerState.Wrong(null, true)))

@Preview(name = "Time's up - ES", locale = "es", device = PREVIEW_DEVICE)
@Composable
private fun PreviewTimeUpEs() = QuizPreview(previewState(QuestionType.MCQ, "es", AnswerState.Wrong(null, true)))
