package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.User
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.flavor_in_sessions
import leaf_log.shared.generated.resources.your_tag
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A flavor and the share of sessions it turned up in, as words and a tertiary bar: one line of a
 * flavor profile. [isCustom] marks a tag the user made.
 */
@Composable
fun FlavorRow(
    label: String,
    count: Int,
    outOf: Int,
    modifier: Modifier = Modifier,
    isCustom: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isCustom) YourTagIcon()
            }
            Text(
                text = pluralStringResource(Res.plurals.flavor_in_sessions, outOf, count, outOf),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(count.toFloat() / outOf)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

/** Marks a tag the user made, beside its name. */
@Composable
internal fun YourTagIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = FeatherIcons.User,
        contentDescription = stringResource(Res.string.your_tag),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(14.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun FlavorRowPreview() {
    LeafLogTheme {
        Column(Modifier.padding(16.dp)) {
            FlavorRow(label = "Floral", count = 38, outOf = 52, onClick = {})
            FlavorRow(label = "Peachy", count = 4, outOf = 52, isCustom = true, onClick = {})
        }
    }
}
