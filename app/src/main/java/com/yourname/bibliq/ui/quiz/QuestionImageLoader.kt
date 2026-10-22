package com.yourname.bibliq.ui.quiz

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.yourname.bibliq.data.repository.ContentRepository

/**
 * Loads a question's picture from the CDN. [imageUrl] is the S3 key from the pack
 * (e.g. "images/q_0004_v1.webp"); Coil's disk cache keeps it available offline.
 */
@Composable
fun QuestionImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ContentRepository.cdnUrl(imageUrl),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.fillMaxSize()
    )
}
