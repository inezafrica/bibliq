package com.yourname.bibliq.data.repository

import com.yourname.bibliq.data.model.LevelPack
import com.yourname.bibliq.data.model.SUPPORTED_LANGUAGES
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class LevelPackValidationTest {

    private lateinit var levelPack: LevelPack
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        val inputStream: InputStream = javaClass.classLoader
            ?.getResourceAsStream("levels/pack_001.json")
            ?: throw IllegalStateException("Could not find pack_001.json in test resources. " +
                    "Make sure it is placed in app/src/test/resources/levels/pack_001.json")

        levelPack = json.decodeFromString(LevelPack.serializer(), inputStream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `pack_001 JSON loads successfully without exceptions`() {
        assertEquals(1, levelPack.packId)
        assertEquals(3, levelPack.levels.size)
        assertEquals(70, levelPack.questions.size)
    }

    @Test
    fun `pack_001 passes all validation rules - no errors`() {
        val errors = LevelPackValidator.validate(levelPack)
        val errorMessage = errors.joinToString("\n") { "  - [${it.field}] ${it.message}" }
        assertTrue(
            "Expected 0 validation errors, but found ${errors.size}:\n$errorMessage",
            errors.isEmpty()
        )
    }

    @Test
    fun `every level has exactly 10 questionIds`() {
        levelPack.levels.forEach { level ->
            assertEquals(
                "Level ${level.number} should have exactly 10 questionIds",
                10,
                level.questionIds.size
            )
        }
    }

    @Test
    fun `every questionId referenced in levels exists in questions list`() {
        val questionIdsSet = levelPack.questions.map { it.id }.toSet()

        levelPack.levels.forEach { level ->
            level.questionIds.forEach { qid ->
                assertTrue(
                    "Level ${level.number} references question '$qid' which does not exist in questions list",
                    questionIdsSet.contains(qid)
                )
            }
        }
    }

    @Test
    fun `no duplicate question ids exist`() {
        val seenIds = mutableSetOf<String>()
        levelPack.questions.forEach { question ->
            assertTrue(
                "Duplicate question id found: ${question.id}",
                seenIds.add(question.id)
            )
        }
    }

    @Test
    fun `correctIndex is always within options range for every question`() {
        levelPack.questions.forEach { question ->
            val maxIndex = question.options.getValue("en").size - 1
            assertTrue(
                "Question ${question.id}: correctIndex ${question.correctIndex} should be between 0 and $maxIndex",
                question.correctIndex in 0..maxIndex
            )
        }
    }

    @Test
    fun `options have the same size across every supported language`() {
        levelPack.questions.forEach { question ->
            val sizes = SUPPORTED_LANGUAGES.map { lang -> lang to question.options.getValue(lang).size }
            val distinctSizes = sizes.map { it.second }.toSet()
            assertEquals(
                "Question ${question.id}: option counts differ across languages: $sizes",
                1,
                distinctSizes.size
            )
        }
    }

    @Test
    fun `every question has text, options and reference for every supported language`() {
        levelPack.questions.forEach { question ->
            SUPPORTED_LANGUAGES.forEach { lang ->
                assertTrue("Question ${question.id} is missing text for '$lang'", question.text.containsKey(lang))
                assertTrue("Question ${question.id} is missing options for '$lang'", question.options.containsKey(lang))
                assertTrue("Question ${question.id} is missing reference for '$lang'", question.reference.containsKey(lang))
            }
        }
    }

    @Test
    fun `IMAGE questions have non-blank imageUrl, others do not`() {
        levelPack.questions.forEach { question ->
            if (question.type == com.yourname.bibliq.data.model.QuestionType.IMAGE) {
                assertNotNull(
                    "Question ${question.id} is IMAGE type but imageUrl is null",
                    question.imageUrl
                )
                assertFalse(
                    "Question ${question.id} is IMAGE type but imageUrl is blank",
                    question.imageUrl!!.isBlank()
                )
            } else {
                assertNull(
                    "Question ${question.id} is ${question.type} but has imageUrl set: ${question.imageUrl}",
                    question.imageUrl
                )
            }
        }
    }

    @Test
    fun `MCQ questions have exactly 4 options in every language`() {
        val mcqQuestions = levelPack.questions.filter {
            it.type == com.yourname.bibliq.data.model.QuestionType.MCQ
        }
        assertTrue("Expected at least 1 MCQ question", mcqQuestions.isNotEmpty())

        mcqQuestions.forEach { question ->
            SUPPORTED_LANGUAGES.forEach { lang ->
                assertEquals(
                    "Question ${question.id} (MCQ) should have exactly 4 options for '$lang', got ${question.options.getValue(lang).size}",
                    4,
                    question.options.getValue(lang).size
                )
            }
        }
    }

    @Test
    fun `TRUE_FALSE questions have exactly 2 options in every language`() {
        val tfQuestions = levelPack.questions.filter {
            it.type == com.yourname.bibliq.data.model.QuestionType.TRUE_FALSE
        }
        assertTrue("Expected at least 1 TRUE_FALSE question", tfQuestions.isNotEmpty())

        tfQuestions.forEach { question ->
            SUPPORTED_LANGUAGES.forEach { lang ->
                assertEquals(
                    "Question ${question.id} (TRUE_FALSE) should have exactly 2 options for '$lang', got ${question.options.getValue(lang).size}",
                    2,
                    question.options.getValue(lang).size
                )
            }
        }
    }

    @Test
    fun `IMAGE questions have exactly 4 options in every language`() {
        val imageQuestions = levelPack.questions.filter {
            it.type == com.yourname.bibliq.data.model.QuestionType.IMAGE
        }
        assertTrue("Expected at least 1 IMAGE question", imageQuestions.isNotEmpty())

        imageQuestions.forEach { question ->
            SUPPORTED_LANGUAGES.forEach { lang ->
                assertEquals(
                    "Question ${question.id} (IMAGE) should have exactly 4 options for '$lang', got ${question.options.getValue(lang).size}",
                    4,
                    question.options.getValue(lang).size
                )
            }
        }
    }

    @Test
    fun `TRUE_FALSE options have correct labels in every supported language`() {
        val expectedLabels = mapOf(
            "en" to listOf("True", "False"),
            "fr" to listOf("Vrai", "Faux"),
            "es" to listOf("Verdadero", "Falso")
        )
        val tfQuestions = levelPack.questions.filter {
            it.type == com.yourname.bibliq.data.model.QuestionType.TRUE_FALSE
        }
        assertTrue("Expected at least 1 TRUE_FALSE question", tfQuestions.isNotEmpty())

        tfQuestions.forEach { question ->
            SUPPORTED_LANGUAGES.forEach { lang ->
                val labels = expectedLabels.getValue(lang)
                assertTrue(
                    "Question ${question.id} '$lang' options should contain $labels. Got: ${question.options[lang]}",
                    question.options.getValue(lang).containsAll(labels)
                )
            }
        }
    }

    @Test
    fun `no text field is blank in any question`() {
        levelPack.questions.forEach { question ->
            assertFalse("Question ${question.id}.id is blank", question.id.isBlank())
            SUPPORTED_LANGUAGES.forEach { lang ->
                assertFalse("Question ${question.id}.text[$lang] is blank", question.text.getValue(lang).isBlank())
                assertFalse("Question ${question.id}.reference[$lang] is blank", question.reference.getValue(lang).isBlank())
            }
        }
    }

    @Test
    fun `all questions are difficulty 1 only`() {
        levelPack.questions.forEach { question ->
            assertEquals(
                "Question ${question.id} should have difficulty 1 only, found ${question.difficulty}",
                1,
                question.difficulty
            )
        }
    }

    @Test
    fun `each level has 5 MCQ, 3 TRUE_FALSE, and 2 IMAGE questions`() {
        val questionsById = levelPack.questions.associateBy { it.id }

        levelPack.levels.forEach { level ->
            val levelQuestions = level.questionIds.mapNotNull { questionsById[it] }
            assertEquals("Level ${level.number} should have exactly 10 questions", 10, levelQuestions.size)

            val mcqCount = levelQuestions.count { it.type == com.yourname.bibliq.data.model.QuestionType.MCQ }
            val tfCount = levelQuestions.count { it.type == com.yourname.bibliq.data.model.QuestionType.TRUE_FALSE }
            val imgCount = levelQuestions.count { it.type == com.yourname.bibliq.data.model.QuestionType.IMAGE }

            assertEquals("Level ${level.number} should have 5 MCQ questions", 5, mcqCount)
            assertEquals("Level ${level.number} should have 3 TRUE_FALSE questions", 3, tfCount)
            assertEquals("Level ${level.number} should have 2 IMAGE questions", 2, imgCount)
        }
    }

    @Test
    fun `correct answer is NOT always at index 0 - mixed positions`() {
        val firstPositions = levelPack.questions.count { it.correctIndex == 0 }
        val total = levelPack.questions.size
        assertTrue(
            "Correct answers are always at index 0 ($firstPositions / $total). Mix them up!",
            firstPositions < total
        )
        val nonZeroCount = total - firstPositions
        assertTrue(
            "Only $nonZeroCount out of $total correct answers are NOT at index 0. Please vary positions more.",
            nonZeroCount >= 10
        )
    }

    @Test
    fun `level pack has correct world numbering and boss flag`() {
        val level1 = levelPack.levels.find { it.number == 1 }
        val level2 = levelPack.levels.find { it.number == 2 }
        val level3 = levelPack.levels.find { it.number == 3 }

        assertNotNull("Level 1 should exist", level1)
        assertNotNull("Level 2 should exist", level2)
        assertNotNull("Level 3 should exist", level3)

        assertEquals("Level 1 should be in World 1", 1, level1!!.world)
        assertEquals("Level 2 should be in World 1", 1, level2!!.world)
        assertEquals("Level 3 should be in World 1", 1, level3!!.world)

        assertFalse("Level 1 should NOT be a boss level", level1.isBoss)
        assertFalse("Level 2 should NOT be a boss level", level2.isBoss)
        assertTrue("Level 3 should be a boss level", level3.isBoss)
    }

    @Test
    fun `starThresholds have exactly 3 values for each level`() {
        levelPack.levels.forEach { level ->
            assertEquals(
                "Level ${level.number} starThresholds should have exactly 3 values",
                3,
                level.starThresholds.size
            )
            val (oneStar, twoStar, threeStar) = level.starThresholds
            assertTrue(
                "Level ${level.number}: 1-star threshold ($oneStar) must be <= 2-star ($twoStar) <= 3-star ($threeStar)",
                oneStar <= twoStar && twoStar <= threeStar
            )
            assertTrue(
                "Level ${level.number}: 3-star threshold must be <= 10 (total questions)",
                threeStar <= 10
            )
        }
    }

    @Test
    fun `all question ids follow q_XXXX format`() {
        val idPattern = Regex("^q_\\d{4}$")
        levelPack.questions.forEach { question ->
            assertTrue(
                "Question id '${question.id}' does not match expected format 'q_XXXX'",
                idPattern.matches(question.id)
            )
        }
    }
}
