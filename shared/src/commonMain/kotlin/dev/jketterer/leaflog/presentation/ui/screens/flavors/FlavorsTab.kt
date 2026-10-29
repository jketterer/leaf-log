package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.FlavorProfile
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorChip
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorRow
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.flavors_intro
import leaf_log.shared.generated.resources.flavors_top
import leaf_log.shared.generated.resources.flavors_top_out_of
import leaf_log.shared.generated.resources.header_flavors_all
import leaf_log.shared.generated.resources.header_flavors_not_tasted
import leaf_log.shared.generated.resources.header_flavors_tasted
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The journal's vocabulary: the most used flavor up top, every tasted one ranked by the sessions
 * it turned up in, then the rest to discover. Before any tagging, the whole vocabulary.
 */
@Composable
fun FlavorsTab(
    state: FlavorsState,
    onFlavorClick: (tagId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    val profile = state.profile
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (profile == null) {
            item(key = "intro") {
                Text(
                    text = stringResource(Res.string.flavors_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item(key = "top") { TopFlavor(profile, modifier = Modifier.padding(bottom = 16.dp)) }
            item(key = "tasted_header") { SectionHeader(stringResource(Res.string.header_flavors_tasted)) }
            items(profile.tags, key = { it.first.id }) { (tag, count) ->
                FlavorRow(
                    label = tag.label(),
                    count = count,
                    outOf = profile.sessions,
                    isCustom = tag.isCustom,
                    onClick = { onFlavorClick(tag.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        val notTasted = state.notTasted
        if (notTasted.isNotEmpty()) {
            item(key = "not_tasted_header") {
                SectionHeader(
                    title = stringResource(if (profile == null) Res.string.header_flavors_all else Res.string.header_flavors_not_tasted),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item(key = "not_tasted") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize().animateItem(),
                ) {
                    notTasted.forEach { tag ->
                        FlavorChip(label = tag.label(), isCustom = tag.isCustom, onClick = { onFlavorClick(tag.id) })
                    }
                }
            }
        }
    }
}

/** The most used flavor, as Recap shows a top flavor: its count large, then what it counts. */
@Composable
private fun TopFlavor(profile: FlavorProfile, modifier: Modifier = Modifier) {
    val (top, count) = profile.tags.first()
    Column(modifier = modifier) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = pluralStringResource(Res.plurals.flavors_top, count, count, top.label()),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = pluralStringResource(Res.plurals.flavors_top_out_of, profile.sessions, profile.sessions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val previewVocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
} + listOf(
    TastingTag(id = "peachy", key = null, name = "Peachy", isCustom = true, displayOrder = 23),
    TastingTag(id = "petrichor", key = null, name = "Petrichor", isCustom = true, displayOrder = 24),
)

@Preview(showBackground = true)
@Composable
private fun FlavorsTabPreview() {
    val byId = previewVocabulary.associateBy { it.id }
    LeafLogTheme {
        FlavorsTab(
            state = FlavorsState(
                vocabulary = previewVocabulary,
                profile = FlavorProfile(
                    tags = listOf("floral" to 38, "roasted" to 21, "peachy" to 4, "mineral" to 3)
                        .map { (id, count) -> byId.getValue(id) to count },
                    sessions = 52,
                ),
                isLoading = false,
            ),
            onFlavorClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FlavorsTabUntaggedPreview() {
    LeafLogTheme {
        FlavorsTab(state = FlavorsState(vocabulary = previewVocabulary, isLoading = false), onFlavorClick = {})
    }
}
