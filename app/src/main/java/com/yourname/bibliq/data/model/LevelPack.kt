package com.yourname.bibliq.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LevelPack(
    val packId: Int,
    val levels: List<Level>,
    val questions: List<Question>
)
