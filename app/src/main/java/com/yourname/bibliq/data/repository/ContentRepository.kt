package com.yourname.bibliq.data.repository

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import com.yourname.bibliq.BuildConfig
import com.yourname.bibliq.data.model.Level
import com.yourname.bibliq.data.model.LevelPack
import com.yourname.bibliq.data.model.LocalizedQuestion
import com.yourname.bibliq.data.model.Manifest
import com.yourname.bibliq.data.model.Question
import com.yourname.bibliq.data.model.localize
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class GameContent(
    val levels: List<Level>,
    val questions: Map<String, Question>,
    val nextDropDate: LocalDate
) {
    fun level(number: Int): Level? = levels.find { it.number == number }

    fun questionsFor(level: Level, language: String): List<LocalizedQuestion> =
        level.questionIds.mapNotNull { questions[it]?.localize(language) }
}

enum class ContentStatus { LOADING, READY, ERROR }

/**
 * Downloads manifest.json and its packs from the CDN and keeps them in filesDir,
 * so levels stay playable offline once fetched. Packs are immutable (their file
 * name embeds the version), so each one is downloaded at most once.
 */
class ContentRepository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val dir = File(context.filesDir, "content")
    private val mutex = Mutex()

    private val _content = MutableStateFlow<GameContent?>(null)
    val content: StateFlow<GameContent?> = _content.asStateFlow()

    private val _status = MutableStateFlow(ContentStatus.LOADING)
    val status: StateFlow<ContentStatus> = _status.asStateFlow()

    suspend fun refresh() = mutex.withLock {
        withContext(Dispatchers.IO) {
            _status.value = ContentStatus.LOADING
            if (_content.value == null) {
                _content.value = runCatching { loadFromDisk() }.getOrNull()
            }
            try {
                syncFromCdn()
                _content.value = loadFromDisk()
                prefetchImages()
                _status.value = ContentStatus.READY
            } catch (e: Exception) {
                Log.w(TAG, "Content sync failed; using cached content if any", e)
                _status.value = if (_content.value != null) ContentStatus.READY else ContentStatus.ERROR
            }
        }
    }

    private fun syncFromCdn() {
        val manifestBytes = download(MANIFEST_FILE)
        val manifest = json.decodeFromString(Manifest.serializer(), manifestBytes.decodeToString())

        for (entry in manifest.packs) {
            val target = File(dir, entry.file)
            if (target.exists()) continue
            val bytes = download(entry.file)
            check(sha256(bytes) == entry.sha256) { "Checksum mismatch for ${entry.file}" }
            val pack = json.decodeFromString(LevelPack.serializer(), bytes.decodeToString())
            val errors = LevelPackValidator.validate(pack)
            check(errors.isEmpty()) { "${entry.file} failed validation: $errors" }
            writeAtomically(target, bytes)
        }

        // Manifest last, so the cache never points at a pack that isn't on disk yet.
        writeAtomically(File(dir, MANIFEST_FILE), manifestBytes)

        val keep = manifest.packs.map { File(dir, it.file).canonicalPath }.toSet()
        File(dir, PACKS_DIR).listFiles()?.filter { it.canonicalPath !in keep }?.forEach { it.delete() }
    }

    private fun loadFromDisk(): GameContent? {
        val manifestFile = File(dir, MANIFEST_FILE)
        if (!manifestFile.exists()) return null
        val manifest = json.decodeFromString(Manifest.serializer(), manifestFile.readText())
        val packs = manifest.packs.map { entry ->
            json.decodeFromString(LevelPack.serializer(), File(dir, entry.file).readText())
        }
        return GameContent(
            levels = packs.flatMap { it.levels }.sortedBy { it.number },
            questions = packs.flatMap { it.questions }.associateBy { it.id },
            nextDropDate = LocalDate.parse(manifest.nextDropDate)
        )
    }

    /** Warms Coil's disk cache so picture questions also work offline. */
    private fun prefetchImages() {
        val loader = context.imageLoader
        _content.value?.questions?.values?.mapNotNull { it.imageUrl }?.forEach { key ->
            loader.enqueue(ImageRequest.Builder(context).data(cdnUrl(key)).build())
        }
    }

    private fun download(path: String): ByteArray {
        val connection = URL(cdnUrl(path)).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.useCaches = false
        try {
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "GET $path returned HTTP $code" }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        check(tmp.renameTo(target) || (target.delete() && tmp.renameTo(target))) { "Could not write $target" }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "ContentRepository"
        private const val MANIFEST_FILE = "manifest.json"
        private const val PACKS_DIR = "packs"
        private const val TIMEOUT_MS = 15_000

        fun cdnUrl(key: String): String = "${BuildConfig.CDN_BASE_URL.trimEnd('/')}/${key.trimStart('/')}"

        @Volatile
        private var instance: ContentRepository? = null

        fun get(context: Context): ContentRepository =
            instance ?: synchronized(this) {
                instance ?: ContentRepository(context.applicationContext).also { instance = it }
            }
    }
}
