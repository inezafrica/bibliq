package com.yourname.bibliq.data.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

enum class NodeState { DONE, CURRENT, LOCKED }

data class LevelNodeUi(
    val number: Int,
    val stars: Int,
    val state: NodeState,
    val isBoss: Boolean
)

data class HomeUiState(
    val rankIndex: Int,
    val rankName: String,
    val xpProgress: Float,
    val lives: Int,
    val secondsToNextLife: Int?,
    val talents: Int,
    val worldName: String,
    val levels: List<LevelNodeUi>,
    val currentLevel: Int,
    val nextDropDate: LocalDate
)

interface HomeRepository {
    fun observeHome(): Flow<HomeUiState>
}
