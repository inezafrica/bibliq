package com.yourname.bibliq.data.repository

import android.content.Context
import com.yourname.bibliq.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow

/**
 * Builds the level map from the downloaded content plus on-device progress.
 * A level unlocks once the previous one has at least one star; the current
 * level is the first unlocked one without stars (or the last level when all are done).
 */
class GameHomeRepository(
    private val context: Context,
    private val content: ContentRepository,
    private val progress: ProgressRepository
) : HomeRepository {

    private val clock: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(CLOCK_TICK_MS)
        }
    }

    override fun observeHome(): Flow<HomeUiState> = combine(
        content.content.filterNotNull(),
        progress.stars,
        progress.balance,
        progress.livesChanged,
        clock
    ) { game, stars, talents, _, _ ->
        val numbers = game.levels.map { it.number }
        val currentLevel = numbers.firstOrNull { (stars[it] ?: 0) == 0 } ?: numbers.lastOrNull() ?: 1

        val nodes = game.levels.map { level ->
            val levelStars = stars[level.number] ?: 0
            LevelNodeUi(
                number = level.number,
                stars = levelStars,
                state = when {
                    level.number == currentLevel -> NodeState.CURRENT
                    levelStars > 0 -> NodeState.DONE
                    else -> NodeState.LOCKED
                },
                isBoss = level.isBoss
            )
        }

        val totalStars = stars.values.sum()
        val rankIndex = (1 + totalStars / STARS_PER_RANK).coerceAtMost(RANK_NAMES.size)
        val lives = progress.livesStatus()
        val world = game.level(currentLevel)?.world ?: 1

        HomeUiState(
            rankIndex = rankIndex,
            rankName = context.getString(RANK_NAMES[rankIndex - 1]),
            xpProgress = (totalStars % STARS_PER_RANK) / STARS_PER_RANK.toFloat(),
            lives = lives.lives,
            secondsToNextLife = lives.secondsToNextLife,
            talents = talents,
            worldName = context.getString(WORLD_NAMES.getOrElse(world - 1) { WORLD_NAMES.first() }),
            levels = nodes,
            currentLevel = currentLevel,
            nextDropDate = game.nextDropDate
        )
    }

    private companion object {
        const val CLOCK_TICK_MS = 15_000L
        const val STARS_PER_RANK = 30
        val RANK_NAMES = listOf(
            R.string.rank_1, R.string.rank_2, R.string.rank_3,
            R.string.rank_4, R.string.rank_5, R.string.rank_6
        )
        val WORLD_NAMES = listOf(R.string.world_1)
    }
}
