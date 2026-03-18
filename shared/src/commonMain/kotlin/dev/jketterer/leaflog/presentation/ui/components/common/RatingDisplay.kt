package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Star
import compose.icons.fontawesomeicons.solid.Star
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun RatingDisplay(
    rating: Float,
    iconsOnly: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating.toInt()) {
                    FontAwesomeIcons.Solid.Star
                } else {
                    FontAwesomeIcons.Regular.Star
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (!iconsOnly) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(${rating})",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingDisplayPreview() {
    LeafLogTheme {
        RatingDisplay(rating = 4.5f)
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingDisplayIconsOnlyPreview() {
    LeafLogTheme {
        RatingDisplay(rating = 3f, iconsOnly = true)
    }
}