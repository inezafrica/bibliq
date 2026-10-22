package com.yourname.bibliq.data.model

import kotlinx.serialization.Serializable

/** Mirrors manifest.json published by the admin (admin/lib/content-types.ts). */
@Serializable
data class Manifest(
    val schemaVersion: Int,
    val minAppVersion: Int,
    val latestLevel: Int,
    val nextDropDate: String,
    val packs: List<ManifestPack>
)

@Serializable
data class ManifestPack(
    val id: Int,
    val file: String,
    val version: Int,
    val levels: List<Int>,
    val sha256: String
)
