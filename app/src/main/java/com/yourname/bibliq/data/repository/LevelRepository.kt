package com.yourname.bibliq.data.repository

import android.content.Context
import com.yourname.bibliq.data.model.Level
import com.yourname.bibliq.data.model.LevelPack
import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.data.model.localize
import kotlinx.serialization.json.Json
import java.io.InputStream

class LevelRepository(private val context: Context? = null) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private var cachedPack: LevelPack? = null

    fun loadPack(packPath: String = "levels/pack_001.json"): LevelPack {
        cachedPack?.let { return it }

        val inputStream: InputStream = context?.assets?.open(packPath)
            ?: throw IllegalStateException("Context is required to load from assets. Use loadPackFromStream() instead for tests.")

        return loadPackFromStream(inputStream).also { cachedPack = it }
    }

    fun loadPackFromStream(inputStream: InputStream): LevelPack {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val pack = json.decodeFromString(LevelPack.serializer(), jsonString)

        val errors = LevelPackValidator.validate(pack)
        if (errors.isNotEmpty()) {
            val errorMessage = errors.joinToString("\n") { "  - [${it.field}] ${it.message}" }
            throw IllegalArgumentException("LevelPack validation failed with ${errors.size} error(s):\n$errorMessage")
        }

        cachedPack = pack
        return pack
    }

    fun getQuestionsForLevel(levelNumber: Int, language: String): List<LocalizedQuestion> {
        val pack = cachedPack ?: loadPack()
        val level = pack.levels.find { it.number == levelNumber }
            ?: throw IllegalArgumentException("Level $levelNumber not found in pack ${pack.packId}")

        val questionById = pack.questions.associateBy { it.id }
        return level.questionIds.mapNotNull { qid ->
            questionById[qid]?.localize(language)
        }
    }

    fun getLevels(): List<Level> {
        val pack = cachedPack ?: loadPack()
        return pack.levels
    }
}
