package com.yourname.bibliq.domain

import com.yourname.bibliq.data.model.QuestionType
import org.junit.Assert.*
import org.junit.Test

class QuizEngineTest {

    // --- Scoring with speed bonus ---

    @Test
    fun `correct answer with time left adds speed bonus`() {
        assertEquals(24, QuizEngine.scoreForCorrectAnswer(secondsLeft = 14))
    }

    @Test
    fun `correct answer with no time left scores base points only`() {
        assertEquals(10, QuizEngine.scoreForCorrectAnswer(secondsLeft = 0))
    }

    @Test
    fun `speed bonus is capped at 20 even with more seconds left`() {
        assertEquals(30, QuizEngine.scoreForCorrectAnswer(secondsLeft = 25))
        assertEquals(20, QuizEngine.speedBonus(secondsLeft = 999))
    }

    @Test
    fun `wrong answer or timeout scores zero`() {
        assertEquals(0, QuizEngine.scoreForWrongOrTimeout())
    }

    @Test
    fun `perfect level awards bonus only when every question was correct`() {
        assertEquals(50, QuizEngine.perfectLevelBonus(correctCount = 10, totalQuestions = 10))
        assertEquals(0, QuizEngine.perfectLevelBonus(correctCount = 9, totalQuestions = 10))
    }

    // --- Skip ---

    @Test
    fun `skip counts as correct with no speed bonus`() {
        assertEquals(10, QuizEngine.scoreForSkip())
    }

    // --- Chances / failing ---

    @Test
    fun `wrong answer costs one chance`() {
        assertEquals(2, QuizEngine.chancesAfterWrongAnswer(chancesLeft = 3))
    }

    @Test
    fun `chances never go below zero`() {
        assertEquals(0, QuizEngine.chancesAfterWrongAnswer(chancesLeft = 0))
    }

    @Test
    fun `level fails once chances reach zero`() {
        assertTrue(QuizEngine.isFailedByChances(chancesLeft = 0))
        assertFalse(QuizEngine.isFailedByChances(chancesLeft = 1))
    }

    // --- Star thresholds ---

    private val thresholds = listOf(7, 9, 10)

    @Test
    fun `star thresholds map correct counts to the right star rating`() {
        assertEquals(0, QuizEngine.starsFor(6, thresholds))
        assertEquals(1, QuizEngine.starsFor(7, thresholds))
        assertEquals(1, QuizEngine.starsFor(8, thresholds))
        assertEquals(2, QuizEngine.starsFor(9, thresholds))
        assertEquals(3, QuizEngine.starsFor(10, thresholds))
    }

    @Test
    fun `below the first star threshold counts as failed even with chances left`() {
        assertTrue(QuizEngine.isLevelFailed(chancesLeft = 2, correctCount = 6, starThresholds = thresholds))
        assertFalse(QuizEngine.isLevelFailed(chancesLeft = 2, correctCount = 7, starThresholds = thresholds))
    }

    @Test
    fun `hitting zero chances counts as failed regardless of correct count`() {
        assertTrue(QuizEngine.isLevelFailed(chancesLeft = 0, correctCount = 8, starThresholds = thresholds))
    }

    // --- Power-ups ---

    @Test
    fun `50-50 is disabled for true-false but allowed for mcq and image`() {
        assertFalse(QuizEngine.is5050Allowed(QuestionType.TRUE_FALSE))
        assertTrue(QuizEngine.is5050Allowed(QuestionType.MCQ))
        assertTrue(QuizEngine.is5050Allowed(QuestionType.IMAGE))
    }

    @Test
    fun `extra time adds 10 seconds`() {
        assertEquals(24, QuizEngine.applyExtraTime(secondsLeft = 14))
    }

    @Test
    fun `power-up talent costs match the spec`() {
        assertEquals(30, PowerUpType.FIFTY_FIFTY.talentCost)
        assertEquals(20, PowerUpType.EXTRA_TIME.talentCost)
        assertEquals(50, PowerUpType.SKIP.talentCost)
        assertEquals(25, PowerUpType.REVEAL_VERSE.talentCost)
    }
}
