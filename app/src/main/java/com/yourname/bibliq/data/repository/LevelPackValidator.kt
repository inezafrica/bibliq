package com.yourname.bibliq.data.repository

import com.yourname.bibliq.data.model.LevelPack
import com.yourname.bibliq.data.model.QuestionType
import com.yourname.bibliq.data.model.SUPPORTED_LANGUAGES

data class ValidationError(
    val field: String,
    val message: String
)

object LevelPackValidator {

    fun validate(levelPack: LevelPack): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        errors.addAll(validateNoDuplicateQuestionIds(levelPack))
        errors.addAll(validateLevels(levelPack))
        errors.addAll(validateQuestions(levelPack))

        return errors
    }

    private fun validateNoDuplicateQuestionIds(levelPack: LevelPack): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val seenIds = mutableSetOf<String>()
        levelPack.questions.forEach { question ->
            if (!seenIds.add(question.id)) {
                errors.add(
                    ValidationError(
                        "questions[${question.id}]",
                        "Duplicate question id: ${question.id}"
                    )
                )
            }
        }
        return errors
    }

    private fun validateLevels(levelPack: LevelPack): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val questionIdsSet = levelPack.questions.map { it.id }.toSet()

        levelPack.levels.forEachIndexed { levelIndex, level ->
            val prefix = "levels[$levelIndex](number=${level.number})"

            if (level.questionIds.size != 10) {
                errors.add(
                    ValidationError(
                        "$prefix.questionIds",
                        "Level must have exactly 10 questionIds, found ${level.questionIds.size}"
                    )
                )
            }

            level.questionIds.forEachIndexed { qIndex, qid ->
                if (qid !in questionIdsSet) {
                    errors.add(
                        ValidationError(
                            "$prefix.questionIds[$qIndex]",
                            "Question id '$qid' not found in questions list"
                        )
                    )
                }
            }
        }

        return errors
    }

    private fun validateQuestions(levelPack: LevelPack): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        levelPack.questions.forEach { question ->
            val prefix = "questions[${question.id}]"

            if (question.id.isBlank()) {
                errors.add(ValidationError("$prefix.id", "Question id cannot be blank"))
            }

            SUPPORTED_LANGUAGES.forEach { lang ->
                val text = question.text[lang]
                if (text == null) {
                    errors.add(ValidationError("$prefix.text[$lang]", "Missing text for language '$lang'"))
                } else if (text.isBlank()) {
                    errors.add(ValidationError("$prefix.text[$lang]", "text for language '$lang' cannot be blank"))
                }

                val reference = question.reference[lang]
                if (reference == null) {
                    errors.add(ValidationError("$prefix.reference[$lang]", "Missing reference for language '$lang'"))
                } else if (reference.isBlank()) {
                    errors.add(ValidationError("$prefix.reference[$lang]", "reference for language '$lang' cannot be blank"))
                }

                val options = question.options[lang]
                if (options == null) {
                    errors.add(ValidationError("$prefix.options[$lang]", "Missing options for language '$lang'"))
                } else if (options.any { it.isBlank() }) {
                    errors.add(ValidationError("$prefix.options[$lang]", "options for language '$lang' cannot contain a blank entry"))
                }
            }

            val optionCountsByLanguage = question.options.mapValues { it.value.size }
            if (optionCountsByLanguage.values.toSet().size > 1) {
                errors.add(
                    ValidationError(
                        "$prefix.options",
                        "Option counts differ across languages: $optionCountsByLanguage"
                    )
                )
            }

            val expectedOptionsCount = when (question.type) {
                QuestionType.MCQ -> 4
                QuestionType.TRUE_FALSE -> 2
                QuestionType.IMAGE -> 4
                else -> null
            }

            expectedOptionsCount?.let { expected ->
                optionCountsByLanguage.forEach { (lang, count) ->
                    if (count != expected) {
                        errors.add(
                            ValidationError(
                                "$prefix.options[$lang]",
                                "${question.type} must have $expected options, found $count"
                            )
                        )
                    }
                }
            }

            val optionsSize = optionCountsByLanguage.values.firstOrNull() ?: 0
            if (question.correctIndex < 0 || question.correctIndex >= optionsSize) {
                errors.add(
                    ValidationError(
                        "$prefix.correctIndex",
                        "correctIndex ${question.correctIndex} is out of range for $optionsSize options (valid: 0-${optionsSize - 1})"
                    )
                )
            }

            when (question.type) {
                QuestionType.IMAGE -> {
                    if (question.imageUrl == null || question.imageUrl.isBlank()) {
                        errors.add(
                            ValidationError(
                                "$prefix.imageUrl",
                                "IMAGE questions must have a non-blank imageUrl"
                            )
                        )
                    }
                }
                else -> {
                    if (question.imageUrl != null) {
                        errors.add(
                            ValidationError(
                                "$prefix.imageUrl",
                                "${question.type} questions should not have imageUrl, but found: ${question.imageUrl}"
                            )
                        )
                    }
                }
            }

            if (question.difficulty < 1 || question.difficulty > 5) {
                errors.add(
                    ValidationError(
                        "$prefix.difficulty",
                        "difficulty must be between 1 and 5, found ${question.difficulty}"
                    )
                )
            }
        }

        return errors
    }
}
