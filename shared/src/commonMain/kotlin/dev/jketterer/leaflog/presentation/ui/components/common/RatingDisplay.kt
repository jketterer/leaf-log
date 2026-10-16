package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Star
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.StarFilled
import dev.jketterer.leaflog.presentation.ui.theme.StarHalf
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.rating_out_of_five
import leaf_log.shared.generated.resources.rating_unrated
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * A rating as five stars to the nearest half, followed by its value unless [iconsOnly]. A null
 * rating reads "Unrated" rather than showing empty stars.
 */
@Composable
fun RatingDisplay(
    rating: Float?,
    modifier: Modifier = Modifier,
    iconsOnly: Boolean = false,
) {
    if (rating == null) {
        Text(
            text = stringResource(Res.string.rating_unrated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val description = ratingDescription(rating)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val halves = halfStars(rating)
        repeat(5) { index ->
            val filled = halves - index * 2
            Icon(
                imageVector = when {
                    filled >= 2 -> FeatherIcons.StarFilled
                    filled == 1 -> FeatherIcons.StarHalf
                    else -> FeatherIcons.Star
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (!iconsOnly) {
            Text(
                text = formatRating(rating),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/** "Rated 4.3 out of 5", or "Unrated" for null. */
@Composable
internal fun ratingDescription(rating: Float?): String =
    if (rating == null) {
        stringResource(Res.string.rating_unrated)
    } else {
        stringResource(Res.string.rating_out_of_five, formatRating(rating))
    }

/** How many half stars [rating] fills, to the nearest half: 4.3 fills nine. */
internal fun halfStars(rating: Float): Int = (rating * 2).roundToInt().coerceIn(0, 10)

/** One decimal, dropped when it's zero: "4.3", "4". */
internal fun formatRating(rating: Float): String {
    val tenths = (rating * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

@Preview(showBackground = true)
@Composable
private fun RatingDisplayPreview() {
    LeafLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RatingDisplay(rating = 4.3f)
            RatingDisplay(rating = 4f)
            RatingDisplay(rating = 2.75f, iconsOnly = true)
            RatingDisplay(rating = null)
        }
    }
}
