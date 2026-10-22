package com.yourname.bibliq.data.repository

import android.content.Context
import com.yourname.bibliq.domain.LevelResult
import com.yourname.bibliq.domain.TalentWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LivesStatus(val lives: Int, val secondsToNextLife: Int?)

data class RecordedResult(val talentsEarned: Int, val lifeLost: Boolean)

/**
 * The player's on-device progress: best stars per level, Talents and lives.
 * Lives follow the game rules: 5 max, a failed level costs one, one regenerates
 * every 30 minutes.
 */
class ProgressRepository private constructor(context: Context) : TalentWallet {

    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    private val _stars = MutableStateFlow(readStars())
    val stars: StateFlow<Map<Int, Int>> = _stars.asStateFlow()

    private val _talents = MutableStateFlow(prefs.getInt(KEY_TALENTS, STARTING_TALENTS))
    override val balance: StateFlow<Int> = _talents.asStateFlow()

    private val _livesVersion = MutableStateFlow(0)
    /** Bumps whenever lives change, so observers recompute [livesStatus]. */
    val livesChanged: StateFlow<Int> = _livesVersion.asStateFlow()

    @Synchronized
    override fun trySpend(amount: Int): Boolean {
        val current = _talents.value
        if (current < amount) return false
        setTalents(current - amount)
        return true
    }

    @Synchronized
    fun livesStatus(now: Long = System.currentTimeMillis()): LivesStatus {
        val (lives, updatedAt) = regenerate(now)
        val secondsToNext = if (lives < MAX_LIVES) {
            ((LIFE_REGEN_MS - (now - updatedAt)) / 1000).toInt().coerceAtLeast(1)
        } else {
            null
        }
        return LivesStatus(lives, secondsToNext)
    }

    @Synchronized
    fun recordResult(result: LevelResult, now: Long = System.currentTimeMillis()): RecordedResult {
        if (result.failed) {
            val (lives, updatedAt) = regenerate(now)
            val newUpdatedAt = if (lives == MAX_LIVES) now else updatedAt
            saveLives((lives - 1).coerceAtLeast(0), newUpdatedAt)
            return RecordedResult(talentsEarned = 0, lifeLost = true)
        }

        val previousBest = _stars.value[result.levelNumber] ?: 0
        if (result.stars <= previousBest) return RecordedResult(talentsEarned = 0, lifeLost = false)

        val earned = (result.stars - previousBest) * TALENTS_PER_NEW_STAR
        val updated = _stars.value + (result.levelNumber to result.stars)
        prefs.edit().putString(KEY_STARS, updated.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
        _stars.value = updated
        setTalents(_talents.value + earned)
        return RecordedResult(talentsEarned = earned, lifeLost = false)
    }

    /** Applies regeneration since the last update and persists it; returns (lives, updatedAt). */
    private fun regenerate(now: Long): Pair<Int, Long> {
        val stored = prefs.getInt(KEY_LIVES, MAX_LIVES)
        val updatedAt = prefs.getLong(KEY_LIVES_UPDATED_AT, now)
        if (stored >= MAX_LIVES) return MAX_LIVES to now

        val regenerated = ((now - updatedAt) / LIFE_REGEN_MS).toInt()
        if (regenerated <= 0) return stored to updatedAt

        val lives = (stored + regenerated).coerceAtMost(MAX_LIVES)
        val newUpdatedAt = if (lives == MAX_LIVES) now else updatedAt + regenerated * LIFE_REGEN_MS
        saveLives(lives, newUpdatedAt)
        return lives to newUpdatedAt
    }

    private fun saveLives(lives: Int, updatedAt: Long) {
        prefs.edit().putInt(KEY_LIVES, lives).putLong(KEY_LIVES_UPDATED_AT, updatedAt).apply()
        _livesVersion.value += 1
    }

    private fun setTalents(value: Int) {
        prefs.edit().putInt(KEY_TALENTS, value).apply()
        _talents.value = value
    }

    private fun readStars(): Map<Int, Int> =
        prefs.getString(KEY_STARS, null)
            ?.split(",")
            ?.mapNotNull { entry ->
                val parts = entry.split(":")
                val level = parts.getOrNull(0)?.toIntOrNull()
                val stars = parts.getOrNull(1)?.toIntOrNull()
                if (level != null && stars != null) level to stars else null
            }
            ?.toMap()
            .orEmpty()

    companion object {
        const val MAX_LIVES = 5
        private const val LIFE_REGEN_MS = 30 * 60 * 1000L
        private const val STARTING_TALENTS = 240
        private const val TALENTS_PER_NEW_STAR = 10

        private const val KEY_STARS = "stars"
        private const val KEY_TALENTS = "talents"
        private const val KEY_LIVES = "lives"
        private const val KEY_LIVES_UPDATED_AT = "lives_updated_at"

        @Volatile
        private var instance: ProgressRepository? = null

        fun get(context: Context): ProgressRepository =
            instance ?: synchronized(this) {
                instance ?: ProgressRepository(context.applicationContext).also { instance = it }
            }
    }
}
