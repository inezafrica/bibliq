package com.yourname.bibliq.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.bibliq.R
import com.yourname.bibliq.data.repository.ContentStatus
import com.yourname.bibliq.data.repository.HomeUiState
import com.yourname.bibliq.data.repository.LevelNodeUi
import com.yourname.bibliq.data.repository.NodeState
import com.yourname.bibliq.ui.theme.BibliQColors
import com.yourname.bibliq.ui.theme.BibliQTheme
import com.yourname.bibliq.ui.theme.Baloo2
import com.yourname.bibliq.ui.theme.Nunito
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onPlayLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val status by viewModel.contentStatus.collectAsState()
    if (state == null && status == ContentStatus.ERROR) {
        ContentErrorScreen(onRetry = viewModel::refresh, modifier = modifier)
        return
    }
    HomeScreenContent(
        state = state,
        modifier = modifier,
        onLevelClick = onPlayLevel,
        onSettingsClick = viewModel::onSettingsClick,
        onNavClick = viewModel::onNavClick
    )
}

@Composable
private fun ContentErrorScreen(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BibliQColors.Parchment)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.content_error_title),
                fontFamily = Baloo2,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = BibliQColors.Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.content_error_body),
                fontFamily = Nunito,
                fontSize = 16.sp,
                color = BibliQColors.StoneText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BibliQColors.RoyalBlue)
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    fontFamily = Baloo2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    state: HomeUiState?,
    modifier: Modifier = Modifier,
    onLevelClick: (Int) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val finishLevelFirstTemplate = stringResource(R.string.finish_level_first)
    val outOfLivesTemplate = stringResource(R.string.out_of_lives)
    val onOutOfLivesClick: () -> Unit = {
        scope.launch {
            snackbarHostState.showSnackbar(
                String.format(Locale.getDefault(), outOfLivesTemplate, formatMmSs(state?.secondsToNextLife ?: 0))
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomBar(onNavClick = onNavClick) }
    ) { innerPadding ->
        if (state == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LevelMap(
                state = state,
                onLevelClick = { number -> if (state.lives > 0) onLevelClick(number) else onOutOfLivesClick() },
                onLockedLevelClick = { number ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            String.format(Locale.getDefault(), finishLevelFirstTemplate, number)
                        )
                    }
                }
            )
            TopBar(state = state, onSettingsClick = onSettingsClick)
            PlayButton(
                state = state,
                onPlayClick = { onLevelClick(state.currentLevel) },
                onOutOfLivesClick = onOutOfLivesClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun TopBar(state: HomeUiState, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = BibliQColors.RoyalBlue,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(rankBadgeRes(state.rankIndex)),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = state.rankName,
                            fontFamily = Baloo2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BibliQColors.RoyalBlueDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(state.xpProgress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BibliQColors.Gold)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(
                                        if (state.lives > 0) R.drawable.ic_lamp_lit else R.drawable.ic_lamp_out
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.lives.toString(),
                                    fontFamily = Baloo2,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = BibliQColors.Ink
                                )
                            }
                            if (state.lives < 5 && state.secondsToNextLife != null) {
                                LivesCountdown(initialSeconds = state.secondsToNextLife)
                            }
                        }
                    }
                    Pill {
                        Image(
                            painter = painterResource(R.drawable.ic_talent),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.talents.toString(),
                            fontFamily = Baloo2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BibliQColors.Ink
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onSettingsClick() })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
private fun LivesCountdown(initialSeconds: Int) {
    var remaining by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    LaunchedEffect(initialSeconds) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }
    Text(
        text = formatMmSs(remaining),
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = BibliQColors.Ink.copy(alpha = 0.7f)
    )
}

private fun formatMmSs(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun rankBadgeRes(index: Int): Int = when (index) {
    1 -> R.drawable.badge_rank_1_sandals
    2 -> R.drawable.badge_rank_2_net
    3 -> R.drawable.badge_rank_3_quill
    4 -> R.drawable.badge_rank_4_staff
    5 -> R.drawable.badge_rank_5_boat
    else -> R.drawable.badge_rank_6_crown
}

// ---------------------------------------------------------------------------
// Level map
// ---------------------------------------------------------------------------

private data class MapLayout(
    val screenWidth: Dp,
    val segmentHeight: Dp,
    val gateHeight: Dp,
    val ribbonWidth: Dp,
    val ribbonHeight: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val totalContentHeight: Dp,
    val backgroundsBottomToTop: List<Int>,
    val levelPoints: Map<Int, DpOffset>
)

private fun computeMapLayout(screenWidth: Dp, levels: List<LevelNodeUi>): MapLayout {
    val segmentHeight = screenWidth * (1920f / 1080f)
    val gateHeight = screenWidth * (560f / 1080f)
    val ribbonWidth = screenWidth * 0.86f
    val ribbonHeight = ribbonWidth * (230f / 900f)

    // Extra clearance so the ribbon never scrolls into the floating Play button's
    // fixed on-screen zone (button height 64dp + 16dp bottom margin, plus buffer).
    val playButtonClearance = 110.dp

    val topPadding = gateHeight + 180.dp
    val bottomPadding = ribbonHeight + 110.dp + playButtonClearance
    val levelSpacing = 120.dp

    val contentHeightNeeded = topPadding + levelSpacing * (levels.size - 1).coerceAtLeast(0) + bottomPadding

    val backgrounds = buildList {
        add(R.drawable.bg_world1_01)
        add(R.drawable.bg_world1_02)
        add(R.drawable.bg_world1_03)
        var cumulative = segmentHeight * 3
        var addThree = true
        while (cumulative < contentHeightNeeded - segmentHeight) {
            add(if (addThree) R.drawable.bg_world1_03 else R.drawable.bg_world1_02)
            addThree = !addThree
            cumulative += segmentHeight
        }
        add(R.drawable.bg_transition_1_2)
    }
    val totalContentHeight = segmentHeight * backgrounds.size

    val level1Y = totalContentHeight - bottomPadding
    val levelPoints = levels.mapIndexed { index, level ->
        val xFraction = 0.5f + 0.18f * sin(index * 0.9f)
        val x = screenWidth * xFraction
        val y = level1Y - levelSpacing * index
        level.number to DpOffset(x, y)
    }.toMap()

    return MapLayout(
        screenWidth = screenWidth,
        segmentHeight = segmentHeight,
        gateHeight = gateHeight,
        ribbonWidth = ribbonWidth,
        ribbonHeight = ribbonHeight,
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        totalContentHeight = totalContentHeight,
        backgroundsBottomToTop = backgrounds,
        levelPoints = levelPoints
    )
}

@Composable
private fun LevelMap(
    state: HomeUiState,
    onLevelClick: (Int) -> Unit,
    onLockedLevelClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout = remember(maxWidth, state.levels) { computeMapLayout(maxWidth, state.levels) }
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val viewportHeight = maxHeight

        LaunchedEffect(layout.totalContentHeight, state.currentLevel) {
            val currentPoint = layout.levelPoints[state.currentLevel] ?: return@LaunchedEffect
            val targetYPx = with(density) { (currentPoint.y - viewportHeight * 0.6f).toPx() }
            val maxScrollPx = with(density) {
                (layout.totalContentHeight - viewportHeight).coerceAtLeast(0.dp).toPx()
            }
            scrollState.scrollTo(targetYPx.toInt().coerceIn(0, maxScrollPx.toInt()))
        }

        Box(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .height(layout.totalContentHeight)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                layout.backgroundsBottomToTop.reversed().forEach { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().height(layout.segmentHeight)
                    )
                }
            }

            state.levels.zipWithNext().forEach { (from, to) ->
                val fromPoint = layout.levelPoints[from.number]
                val toPoint = layout.levelPoints[to.number]
                if (fromPoint != null && toPoint != null) {
                    val dimmed = from.state == NodeState.LOCKED && to.state == NodeState.LOCKED
                    FootprintPath(from = fromPoint, to = toPoint, dimmed = dimmed)
                }
            }

            state.levels.forEach { level ->
                val point = layout.levelPoints[level.number]
                if (point != null) {
                    LevelNode(
                        level = level,
                        center = point,
                        onClick = { onLevelClick(level.number) },
                        onLockedClick = { onLockedLevelClick(level.number) }
                    )
                }
            }

            layout.levelPoints[state.currentLevel]?.let { currentPoint ->
                val onRight = (currentPoint.x / layout.screenWidth) < 0.5f
                val sideSign = if (onRight) 1f else -1f
                var oliX = currentPoint.x + 110.dp * sideSign
                val leftLimit = layout.screenWidth * 0.22f + 48.dp
                if (oliX < leftLimit) oliX = leftLimit
                OliMascot(center = DpOffset(oliX, currentPoint.y))
            }

            val level1Point = layout.levelPoints[state.levels.firstOrNull()?.number ?: 1]
            if (level1Point != null) {
                WorldRibbon(
                    worldName = state.worldName,
                    topLeft = DpOffset(level1Point.x - layout.ribbonWidth / 2, level1Point.y + 90.dp),
                    size = DpOffset(layout.ribbonWidth, layout.ribbonHeight)
                )
            }

            EndGate(
                nextDropDate = state.nextDropDate,
                topY = 20.dp,
                screenWidth = layout.screenWidth,
                gateHeight = layout.gateHeight
            )
        }
    }
}

@Composable
private fun FootprintPath(from: DpOffset, to: DpOffset, dimmed: Boolean) {
    val dx = (to.x - from.x).value
    val dy = (to.y - from.y).value
    val distance = sqrt(dx * dx + dy * dy)
    if (distance <= 88f) return

    val ux = dx / distance
    val uy = dy / distance
    val angleDeg = Math.toDegrees(atan2(-ux.toDouble(), -uy.toDouble())).toFloat()
    val perpX = -uy
    val perpY = ux
    val alpha = if (dimmed) 0.5f else 1f

    val usableLength = distance - 88f
    val stepCount = (usableLength / 20f).toInt()

    for (i in 0..stepCount) {
        val t = 44f + i * 20f
        val px = from.x.value + ux * t
        val py = from.y.value + uy * t
        val mirrored = i % 2 == 1
        val sideSign = if (mirrored) -1f else 1f
        val offsetX = px + perpX * 6f * sideSign
        val offsetY = py + perpY * 6f * sideSign

        Image(
            painter = painterResource(R.drawable.path_footprint),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .offset(x = offsetX.dp - 8.dp, y = offsetY.dp - 8.dp)
                .graphicsLayer(
                    scaleX = if (mirrored) -1f else 1f,
                    rotationZ = angleDeg,
                    alpha = alpha
                )
        )
    }
}

@Composable
private fun LevelNode(
    level: LevelNodeUi,
    center: DpOffset,
    onClick: () -> Unit,
    onLockedClick: () -> Unit
) {
    val sizeDp = when {
        level.isBoss -> 84.dp
        level.state == NodeState.CURRENT -> 72.dp
        else -> 64.dp
    }

    val contentDesc = when (level.state) {
        NodeState.DONE -> stringResource(R.string.level_cd_done, level.number, level.stars)
        NodeState.CURRENT -> stringResource(R.string.level_cd_current, level.number)
        NodeState.LOCKED -> stringResource(R.string.level_cd_locked, level.number)
    }

    Box(
        modifier = Modifier
            .offset(x = center.x - sizeDp / 2, y = center.y - sizeDp / 2)
            .size(sizeDp)
    ) {
        if (level.state == NodeState.CURRENT) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            Box(
                modifier = Modifier
                    .size(sizeDp * 1.7f)
                    .align(Alignment.Center)
                    .offset(x = -(sizeDp * 0.35f), y = -(sizeDp * 0.35f))
                    .clip(CircleShape)
                    .background(BibliQColors.RoyalBlue.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .pointerInput(level.number) { detectTapGestures(onTap = { onClick() }) }
                    .semantics { contentDescription = contentDesc },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.node_stone_current),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = level.number.toString(),
                    fontFamily = Baloo2,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = BibliQColors.RoyalBlue
                )
            }
        } else {
            val nodeRes = when {
                level.isBoss -> R.drawable.node_stone_boss
                level.state == NodeState.DONE -> R.drawable.node_stone_done
                else -> R.drawable.node_scroll_locked
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (level.state == NodeState.LOCKED) 0.9f else 1f)
                    .pointerInput(level.number, level.state) {
                        detectTapGestures(onTap = {
                            if (level.state == NodeState.LOCKED) onLockedClick() else onClick()
                        })
                    }
                    .semantics { contentDescription = contentDesc },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(nodeRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                if (level.state != NodeState.LOCKED) {
                    Text(
                        text = level.number.toString(),
                        fontFamily = Baloo2,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = BibliQColors.StoneText
                    )
                }
            }

            if (level.state == NodeState.DONE) {
                StarsRow(
                    stars = level.stars,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -16.dp)
                )
            }
        }

        if (level.isBoss) {
            Image(
                painter = painterResource(R.drawable.chest_treasure_closed),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 64.dp, height = 64.dp * 300f / 340f)
                    .align(Alignment.CenterEnd)
                    .offset(x = 72.dp)
            )
        }
    }
}

@Composable
private fun StarsRow(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            Image(
                painter = painterResource(
                    if (i < stars) R.drawable.ic_star_filled else R.drawable.ic_star_empty
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OliMascot(center: DpOffset) {
    val infiniteTransition = rememberInfiniteTransition(label = "oliBob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oliBobOffset"
    )
    Image(
        painter = painterResource(R.drawable.oli_idle),
        contentDescription = null,
        modifier = Modifier
            .size(96.dp)
            .offset(x = center.x - 48.dp, y = center.y - 48.dp + bobOffset.dp)
    )
}

@Composable
private fun WorldRibbon(worldName: String, topLeft: DpOffset, size: DpOffset) {
    Box(
        modifier = Modifier
            .offset(x = topLeft.x, y = topLeft.y)
            .size(size.x, size.y),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ribbon_world),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        ShrinkToFitText(
            text = worldName,
            maxFontSize = 20.sp,
            minFontSize = 12.sp,
            color = BibliQColors.SignText,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun EndGate(nextDropDate: LocalDate, topY: Dp, screenWidth: Dp, gateHeight: Dp) {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val dateText = remember(nextDropDate, locale) { nextDropDate.format(formatter) }
    val label = stringResource(R.string.new_levels_on, dateText)

    Box(
        modifier = Modifier
            .offset(y = topY)
            .width(screenWidth)
            .height(gateHeight)
    ) {
        Image(
            painter = painterResource(R.drawable.gate_walls),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(width = 110.dp, height = 110.dp * 360f / 300f)
        ) {
            Image(
                painter = painterResource(R.drawable.sign_wood),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = label,
                fontFamily = Baloo2,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                color = BibliQColors.Parchment,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 78.dp)
                    .graphicsLayer(rotationZ = -3f)
            )
        }
    }
}

@Composable
private fun ShrinkToFitText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(text) { mutableFloatStateOf(maxFontSize.value) }
    Text(
        text = text,
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize.sp,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize.value) {
                fontSize -= 1f
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Play button
// ---------------------------------------------------------------------------

@Composable
private fun PlayButton(
    state: HomeUiState,
    onPlayClick: () -> Unit,
    onOutOfLivesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val faceOffsetY by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "playButtonPress")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(68.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(20.dp))
                .background(BibliQColors.OliveDark)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.TopCenter)
                .offset(y = faceOffsetY)
                .clip(RoundedCornerShape(20.dp))
                .background(BibliQColors.OliveGreen)
                .pointerInput(state.lives, state.currentLevel) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = {
                            if (state.lives > 0) onPlayClick() else onOutOfLivesClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.lives > 0) {
                ShrinkToFitText(
                    text = stringResource(R.string.play_level, state.currentLevel),
                    maxFontSize = 20.sp,
                    minFontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_lamp_out),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutOfLivesLabel(seconds = state.secondsToNextLife ?: 0)
                }
            }
        }
    }
}

@Composable
private fun OutOfLivesLabel(seconds: Int) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    LaunchedEffect(seconds) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }
    ShrinkToFitText(
        text = stringResource(R.string.next_life_in, formatMmSs(remaining)),
        maxFontSize = 20.sp,
        minFontSize = 14.sp,
        color = Color.White
    )
}

// ---------------------------------------------------------------------------
// Bottom navigation
// ---------------------------------------------------------------------------

private data class NavItem(val route: String, val iconRes: Int, val labelRes: Int)

private val NAV_ITEMS = listOf(
    NavItem("map", R.drawable.ic_nav_map, R.string.nav_map),
    NavItem("endless", R.drawable.ic_nav_endless, R.string.nav_endless),
    NavItem("badges", R.drawable.ic_nav_badges, R.string.nav_badges),
    NavItem("profile", R.drawable.ic_nav_profile, R.string.nav_profile)
)

@Composable
private fun BottomBar(onNavClick: (String) -> Unit, selected: String = "map") {
    NavigationBar(containerColor = Color.White) {
        NAV_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = selected == item.route,
                onClick = { onNavClick(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BibliQColors.RoyalBlue,
                    unselectedIconColor = BibliQColors.NavInactive,
                    selectedTextColor = BibliQColors.RoyalBlue,
                    unselectedTextColor = BibliQColors.NavInactive,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun previewState(): HomeUiState = HomeUiState(
    rankIndex = 2,
    rankName = stringResource(R.string.rank_2),
    xpProgress = 0.4f,
    lives = 3,
    secondsToNextLife = 754,
    talents = 240,
    worldName = stringResource(R.string.world_1),
    levels = (1..18).map { number ->
        LevelNodeUi(
            number = number,
            stars = when (number) {
                1 -> 3
                2 -> 2
                else -> 0
            },
            state = when {
                number <= 2 -> NodeState.DONE
                number == 3 -> NodeState.CURRENT
                else -> NodeState.LOCKED
            },
            isBoss = number % 15 == 0
        )
    },
    currentLevel = 3,
    nextDropDate = LocalDate.now().plusDays(3)
)

@Preview(name = "Home - EN", locale = "en", device = "spec:width=411dp,height=891dp", showBackground = true)
@Composable
private fun HomeScreenPreviewEn() {
    BibliQTheme { HomeScreenContent(state = previewState()) }
}

@Preview(name = "Home - FR", locale = "fr", device = "spec:width=411dp,height=891dp", showBackground = true)
@Composable
private fun HomeScreenPreviewFr() {
    BibliQTheme { HomeScreenContent(state = previewState()) }
}

@Preview(name = "Home - ES", locale = "es", device = "spec:width=411dp,height=891dp", showBackground = true)
@Composable
private fun HomeScreenPreviewEs() {
    BibliQTheme { HomeScreenContent(state = previewState()) }
}
