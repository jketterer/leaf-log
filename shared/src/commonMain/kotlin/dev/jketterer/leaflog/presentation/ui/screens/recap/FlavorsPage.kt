package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_new_flavor
import leaf_log.shared.generated.resources.recap_top_flavor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FlavorsPage(flavors: RecapPage.Flavors) {
    RecapPageColumn {
        val (top, topCount) = flavors.profile.tags.first()
        Text(
            text = topCount.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(Res.plurals.recap_top_flavor, topCount, top.label()),
            style = MaterialTheme.typography.titleLarge,
        )
        if (top.id in flavors.newTagIds) NewBadge(modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        flavors.profile.tags.drop(1).forEach { (tag, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tag.label(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (tag.id in flavors.newTagIds) NewBadge(modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

/** Marks a flavor missing from the period before's. */
@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.recap_new_flavor),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = modifier
            .border(1.dp, LocalContentColor.current, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview
@Composable
private fun FlavorsPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 4) {
            FlavorsPage(
                flavors = RecapPage.Flavors(
                    profile = FlavorProfile(
                        tags = listOf("floral" to 9, "returning_sweetness" to 7, "smooth" to 5, "smoky" to 2).map { (key, count) ->
                            TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = CURATED_TAG_KEYS.indexOf(key)) to count
                        },
                        sessions = 14,
                    ),
                    newTagIds = setOf("returning_sweetness", "smoky"),
                ),
            )
        }
    }
}
