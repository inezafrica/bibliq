package com.yourname.bibliq.data.repository

import com.yourname.bibliq.data.model.QuestionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class LevelRepositoryTest {

    private lateinit var repository: LevelRepository
    private lateinit var packStream: InputStream

    @Before
    fun setup() {
        repository = LevelRepository()
        packStream = javaClass.classLoader
            ?.getResourceAsStream("levels/pack_001.json")
            ?: throw IllegalStateException("Could not find pack_001.json in test resources")
    }

    @Test
    fun `loadPackFromStream loads and validates pack successfully`() {
        val pack = repository.loadPackFromStream(packStream)
        assertEquals(1, pack.packId)
        assertEquals(3, pack.levels.size)
        assertEquals(70, pack.questions.size)
    }

    @Test
    fun `getQuestionsForLevel returns exactly 10 questions for each level in English`() {
        repository.loadPackFromStream(packStream)

        val level1Questions = repository.getQuestionsForLevel(1, "en")
        val level2Questions = repository.getQuestionsForLevel(2, "en")
        val level3Questions = repository.getQuestionsForLevel(3, "en")

        assertEquals("Level 1 should have 10 questions", 10, level1Questions.size)
        assertEquals("Level 2 should have 10 questions", 10, level2Questions.size)
        assertEquals("Level 3 should have 10 questions", 10, level3Questions.size)
    }

    @Test
    fun `getQuestionsForLevel returns exactly 10 questions for each level in French`() {
        repository.loadPackFromStream(packStream)

        val level1Questions = repository.getQuestionsForLevel(1, "fr")
        val level2Questions = repository.getQuestionsForLevel(2, "fr")
        val level3Questions = repository.getQuestionsForLevel(3, "fr")

        assertEquals("Level 1 should have 10 questions (FR)", 10, level1Questions.size)
        assertEquals("Level 2 should have 10 questions (FR)", 10, level2Questions.size)
        assertEquals("Level 3 should have 10 questions (FR)", 10, level3Questions.size)
    }

    @Test
    fun `getQuestionsForLevel returns exactly 10 questions for each level in Spanish`() {
        repository.loadPackFromStream(packStream)

        val level1Questions = repository.getQuestionsForLevel(1, "es")
        val level2Questions = repository.getQuestionsForLevel(2, "es")
        val level3Questions = repository.getQuestionsForLevel(3, "es")

        assertEquals("Level 1 should have 10 questions (ES)", 10, level1Questions.size)
        assertEquals("Level 2 should have 10 questions (ES)", 10, level2Questions.size)
        assertEquals("Level 3 should have 10 questions (ES)", 10, level3Questions.size)
    }

    @Test
    fun `getQuestionsForLevel with French language localizes text and options`() {
        repository.loadPackFromStream(packStream)

        val level1Fr = repository.getQuestionsForLevel(1, "fr")

        level1Fr.forEach { question ->
            assertFalse(
                "Question ${question.id} should have non-empty localized French text",
                question.text.isBlank()
            )
            assertTrue(
                "Question ${question.id} options should have French labels (found English 'True' or 'False' in FR mode)",
                !(question.type == QuestionType.TRUE_FALSE &&
                        question.options.containsAll(listOf("True", "False")))
            )
        }
    }

    @Test
    fun `getQuestionsForLevel with English language keeps original English text`() {
        repository.loadPackFromStream(packStream)

        val level1En = repository.getQuestionsForLevel(1, "en")

        val tfQuestion = level1En.find { it.type == QuestionType.TRUE_FALSE }
        assertNotNull("Should have at least one TRUE_FALSE question in level 1", tfQuestion)
        assertTrue(
            "TRUE_FALSE options should be English 'True'/'False' in EN mode",
            tfQuestion!!.options.containsAll(listOf("True", "False"))
        )
    }

    @Test
    fun `getQuestionsForLevel falls back to English for an unsupported language`() {
        repository.loadPackFromStream(packStream)

        val level1Unsupported = repository.getQuestionsForLevel(1, "pt")
        val level1English = repository.getQuestionsForLevel(1, "en")

        assertEquals(level1English.map { it.text }, level1Unsupported.map { it.text })
    }

    @Test
    fun `getQuestionsForLevel throws for non-existent level`() {
        repository.loadPackFromStream(packStream)

        assertThrows(IllegalArgumentException::class.java) {
            repository.getQuestionsForLevel(999, "en")
        }
    }

    @Test
    fun `getLevels returns all 3 levels after loading`() {
        repository.loadPackFromStream(packStream)
        val levels = repository.getLevels()
        assertEquals(3, levels.size)
        assertEquals(1, levels[0].number)
        assertEquals(2, levels[1].number)
        assertEquals(3, levels[2].number)
    }

    @Test
    fun `getLevels throws if pack not loaded yet`() {
        val freshRepo = LevelRepository()
        assertThrows(IllegalStateException::class.java) {
            freshRepo.getLevels()
        }
    }
}
