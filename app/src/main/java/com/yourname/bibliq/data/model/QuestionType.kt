package com.yourname.bibliq.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuestionType {
    MCQ,
    TRUE_FALSE,
    IMAGE,
    WHO_SAID,
    WHICH_BOOK,
    COMPLETE_VERSE,
    ORDER
}
