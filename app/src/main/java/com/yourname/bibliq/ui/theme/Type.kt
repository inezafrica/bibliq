package com.yourname.bibliq.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.yourname.bibliq.R

// baloo2_bold.ttf / baloo2_extrabold.ttf / nunito_regular.ttf / nunito_bold.ttf are
// placeholders copied from Google Fonts' variable-font releases, pinned to the right
// weight via variationSettings. Swap them for real static-weight exports whenever ready
// (see documentation) -- FontFamily usage below does not need to change.

@OptIn(ExperimentalTextApi::class)
val Baloo2 = FontFamily(
    Font(
        resId = R.font.baloo2_bold,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.baloo2_extrabold,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(
    Font(
        resId = R.font.nunito_regular,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.nunito_bold,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.nunito_extrabold,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

@OptIn(ExperimentalTextApi::class)
val LoraMediumItalic = FontFamily(
    Font(
        resId = R.font.lora_medium_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    )
)
