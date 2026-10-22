package com.yourname.bibliq.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.bibliq.R
import com.yourname.bibliq.domain.LevelResult
import com.yourname.bibliq.ui.theme.Baloo2
import com.yourname.bibliq.ui.theme.BibliQColors
import com.yourname.bibliq.ui.theme.BibliQTheme
import com.yourname.bibliq.ui.theme.Nunito

@Composable
fun LevelResultScreen(
    result: LevelResult,
    totalQuestions: Int,
    talentsEarned: Int,
    lifeLost: Boolean,
    hasNextLevel: Boolean,
    canPlay: Boolean,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToMap)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BibliQColors.ScreenSky)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BibliQColors.Parchment, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (result.failed) R.string.level_failed else R.string.level_complete,
                    result.levelNumber
                ),
                fontFamily = Baloo2,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = if (result.failed) BibliQColors.CoralDark else BibliQColors.RoyalBlueDark,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val filled = !result.failed && index < result.stars
                    Image(
                        painter = painterResource(if (filled) R.drawable.ic_star_filled else R.drawable.ic_star_empty),
                        contentDescription = null,
                        modifier = Modifier.size(if (index == 1) 64.dp else 52.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.result_correct, result.correctCount, totalQuestions),
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BibliQColors.Ink
            )
            Text(
                text = stringResource(R.string.result_score, result.score),
                fontFamily = Nunito,
                fontSize = 16.sp,
                color = BibliQColors.StoneText
            )

            if (talentsEarned > 0) {
                Spacer(Modifier.height(12.dp))
                RewardRow(R.drawable.ic_talent, stringResource(R.string.result_talents, talentsEarned), BibliQColors.GoldDark)
            }
            if (lifeLost) {
                Spacer(Modifier.height(12.dp))
                RewardRow(R.drawable.ic_lamp_out, stringResource(R.string.result_life_lost), BibliQColors.CoralDark)
            }

            Spacer(Modifier.height(28.dp))
            val goNext = !result.failed && hasNextLevel && canPlay
            val retryFirst = result.failed && canPlay
            when {
                goNext -> PrimaryButton(stringResource(R.string.next_level), onNextLevel)
                retryFirst -> PrimaryButton(stringResource(R.string.try_again), onRetry)
                else -> PrimaryButton(stringResource(R.string.back_to_map), onBackToMap)
            }

            Spacer(Modifier.height(12.dp))
            when {
                goNext || retryFirst -> SecondaryButton(stringResource(R.string.back_to_map), onBackToMap)
                result.stars < 3 && canPlay -> SecondaryButton(stringResource(R.string.try_again), onRetry)
            }
        }
    }
}

@Composable
private fun RewardRow(iconRes: Int, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BibliQColors.OliveGreen)
    ) {
        Text(text = text, fontFamily = Baloo2, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontFamily = Baloo2,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BibliQColors.RoyalBlue
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelResultPreview() {
    BibliQTheme {
        LevelResultScreen(
            result = LevelResult(levelNumber = 1, correctCount = 9, score = 214, stars = 2, failed = false),
            totalQuestions = 10,
            talentsEarned = 20,
            lifeLost = false,
            hasNextLevel = true,
            canPlay = true,
            onNextLevel = {},
            onRetry = {},
            onBackToMap = {}
        )
    }
}
