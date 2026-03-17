package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Star
import compose.icons.fontawesomeicons.solid.Star

/**
 * Star rating selector (0-5 stars).
 * Tapping a star sets that rating, tapping current rating clears it.
 */
@Composable
fun RatingSelector(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (1..5).forEach { star ->
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    // Toggle: if clicked star is current rating, set to 0, otherwise set to star
                    onRatingChange(if (rating == star.toFloat()) 0f else star.toFloat())
                },
            ) {
                Icon(
                    imageVector = if (star <= rating) {
                        FontAwesomeIcons.Solid.Star
                    } else {
                        FontAwesomeIcons.Regular.Star
                    },
                    contentDescription = "Star",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}