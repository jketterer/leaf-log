package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Star
import dev.jketterer.leaflog.presentation.ui.components.common.ratingDescription
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.StarFilled
import kotlin.math.roundToInt

/**
 * Whole-star rating input, where 0 means unrated. Tapping a star sets that rating; tapping the
 * current rating clears it. A screen reader hears one slider, adjusted a star at a time.
 */
@Composable
fun RatingSelector(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 24.dp,
) {
    val description = ratingDescription(rating.takeIf { it > 0f })
    Row(
        modifier = modifier.clearAndSetSemantics {
            stateDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(rating, 0f..5f, steps = 4)
            setProgress { value ->
                onRatingChange(value.roundToInt().coerceIn(0, 5).toFloat())
                true
            }
        },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (1..5).forEach { star ->
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    onRatingChange(if (rating == star.toFloat()) 0f else star.toFloat())
                },
            ) {
                Icon(
                    imageVector = if (star <= rating) {
                        FeatherIcons.StarFilled
                    } else {
                        FeatherIcons.Star
                    },
                    contentDescription = null,
                    modifier = Modifier.size(starSize),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingSelectorPreview() {
    var rating by remember { mutableFloatStateOf(3f) }
    LeafLogTheme {
        RatingSelector(
            rating = rating,
            onRatingChange = { rating = it },
        )
    }
}
