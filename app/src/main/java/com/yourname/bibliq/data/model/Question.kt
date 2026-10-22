package com.yourname.bibliq.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String,
    val type: QuestionType,
    val difficulty: Int,
    val text: Map<String, String>,
    val options: Map<String, List<String>>,
    val correctIndex: Int,
    val imageUrl: String? = null,
    val reference: Map<String, String>
)

data class LocalizedQuestion(
    val id: String,
    val type: QuestionType,
    val difficulty: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val imageUrl: String?,
    val reference: String
)

fun Question.localize(language: String): LocalizedQuestion = LocalizedQuestion(
    id = id,
    type = type,
    difficulty = difficulty,
    text = text[language] ?: text[FALLBACK_LANGUAGE].orEmpty(),
    options = options[language] ?: options[FALLBACK_LANGUAGE].orEmpty(),
    correctIndex = correctIndex,
    imageUrl = imageUrl,
    reference = reference[language] ?: reference[FALLBACK_LANGUAGE].orEmpty()
)
