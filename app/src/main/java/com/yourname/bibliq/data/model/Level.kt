package com.yourname.bibliq.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Level(
    val number: Int,
    val world: Int,
    val isBoss: Boolean,
    val starThresholds: List<Int>,
    val questionIds: List<String>
)
