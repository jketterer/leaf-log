package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.tag_astringent
import leaf_log.shared.generated.resources.tag_bitter
import leaf_log.shared.generated.resources.tag_drying
import leaf_log.shared.generated.resources.tag_earthy
import leaf_log.shared.generated.resources.tag_floral
import leaf_log.shared.generated.resources.tag_fruity
import leaf_log.shared.generated.resources.tag_full
import leaf_log.shared.generated.resources.tag_grassy
import leaf_log.shared.generated.resources.tag_honey
import leaf_log.shared.generated.resources.tag_light
import leaf_log.shared.generated.resources.tag_lingering
import leaf_log.shared.generated.resources.tag_malty
import leaf_log.shared.generated.resources.tag_mineral
import leaf_log.shared.generated.resources.tag_nutty
import leaf_log.shared.generated.resources.tag_returning_sweetness
import leaf_log.shared.generated.resources.tag_roasted
import leaf_log.shared.generated.resources.tag_short
import leaf_log.shared.generated.resources.tag_smoky
import leaf_log.shared.generated.resources.tag_smooth
import leaf_log.shared.generated.resources.tag_spicy
import leaf_log.shared.generated.resources.tag_sweet
import leaf_log.shared.generated.resources.tag_umami
import leaf_log.shared.generated.resources.tag_woody
import leaf_log.shared.generated.resources.tags_add
import leaf_log.shared.generated.resources.tags_carried_over
import leaf_log.shared.generated.resources.tags_create
import leaf_log.shared.generated.resources.tags_search
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Each curated tag's name, by its key. */
internal val CURATED_TAG_NAMES: Map<String, StringResource> = mapOf(
    "floral" to Res.string.tag_floral,
    "fruity" to Res.string.tag_fruity,
    "honey" to Res.string.tag_honey,
    "roasted" to Res.string.tag_roasted,
    "smoky" to Res.string.tag_smoky,
    "grassy" to Res.string.tag_grassy,
    "woody" to Res.string.tag_woody,
    "earthy" to Res.string.tag_earthy,
    "spicy" to Res.string.tag_spicy,
    "sweet" to Res.string.tag_sweet,
    "bitter" to Res.string.tag_bitter,
    "astringent" to Res.string.tag_astringent,
    "umami" to Res.string.tag_umami,
    "mineral" to Res.string.tag_mineral,
    "malty" to Res.string.tag_malty,
    "nutty" to Res.string.tag_nutty,
    "light" to Res.string.tag_light,
    "smooth" to Res.string.tag_smooth,
    "full" to Res.string.tag_full,
    "lingering" to Res.string.tag_lingering,
    "short" to Res.string.tag_short,
    "drying" to Res.string.tag_drying,
    "returning_sweetness" to Res.string.tag_returning_sweetness,
)

/** A curated tag's name in the user's language, or a custom tag's own. */
@Composable
fun TastingTag.label(): String =
    key?.let { CURATED_TAG_NAMES[it] }?.let { stringResource(it) } ?: name.orEmpty()

/**
 * The tags whose label contains [query], in order, and whether [query] can be created: it isn't
 * blank and no tag is already called that.
 */
internal fun <T> searchTags(
    labels: List<Pair<T, String>>,
    query: String,
): Pair<List<Pair<T, String>>, Boolean> {
    val word = query.trim()
    val matches = labels.filter { it.second.contains(word, ignoreCase = true) }
    val creatable = word.isNotEmpty() && labels.none { it.second.equals(word, ignoreCase = true) }
    return matches to creatable
}

/**
 * A steep's tags, with an Add chip that opens the whole vocabulary. Tags carried over from the
 * steep before are faded until tapped; tapping a confirmed one removes it.
 */
@Composable
fun SteepTags(
    tags: List<SteepTag>,
    vocabulary: List<TastingTag>,
    onToggle: (tagId: String) -> Unit,
    onCreate: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    val confirmed = tags.associate { it.tagId to it.confirmed }

    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vocabulary.filter { it.id in confirmed }.forEach { tag ->
            TagChip(tag.label(), confirmed[tag.id], onClick = { onToggle(tag.id) })
        }
        AssistChip(
            onClick = { picking = true },
            label = { Text(stringResource(Res.string.tags_add)) },
            leadingIcon = {
                Icon(FeatherIcons.Plus, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
        )
    }

    if (picking) {
        TagPickerSheet(tags, vocabulary, onToggle, onCreate, onDismiss = { picking = false })
    }
}

/** The steep's tags as small chips to read, not tap; carried-over ones faded. */
@Composable
fun SteepTagChips(
    tags: List<SteepTag>,
    vocabulary: List<TastingTag>,
    modifier: Modifier = Modifier,
) {
    val confirmed = tags.associate { it.tagId to it.confirmed }
    val carriedOver = stringResource(Res.string.tags_carried_over)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        vocabulary.filter { it.id in confirmed }.forEach { tag ->
            ReadingTagChip(
                label = tag.label(),
                modifier = if (confirmed[tag.id] == false) {
                    Modifier.alpha(0.5f).semantics { stateDescription = carriedOver }
                } else Modifier,
            )
        }
    }
}

/** [tags] as small chips to read, in the order given, such as a session's dominant tags. */
@Composable
fun TagChips(
    tags: List<TastingTag>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { ReadingTagChip(it.label()) }
    }
}

/** Tertiary, so a tag reads apart from the secondary parameter chips beside it. */
@Composable
private fun ReadingTagChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** [confirmed] is null for a tag the steep doesn't have, false for one carried over. */
@Composable
private fun TagChip(label: String, confirmed: Boolean?, onClick: () -> Unit) {
    val carriedOver = stringResource(Res.string.tags_carried_over)
    FilterChip(
        selected = confirmed != null,
        onClick = onClick,
        label = { Text(label) },
        modifier = if (confirmed == false) {
            Modifier.alpha(0.5f).semantics { stateDescription = carriedOver }
        } else Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSheet(
    tags: List<SteepTag>,
    vocabulary: List<TastingTag>,
    onToggle: (tagId: String) -> Unit,
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        TagPickerContent(tags, vocabulary, onToggle, onCreate)
    }
}

@Composable
private fun TagPickerContent(
    tags: List<SteepTag>,
    vocabulary: List<TastingTag>,
    onToggle: (tagId: String) -> Unit,
    onCreate: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val confirmed = tags.associate { it.tagId to it.confirmed }
    val labels = vocabulary.map { it to it.label() }
    val (matches, creatable) = searchTags(labels, query)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(Res.string.tags_search)) },
            leadingIcon = { Icon(FeatherIcons.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            matches.forEach { (tag, label) ->
                TagChip(label, confirmed[tag.id], onClick = { onToggle(tag.id) })
            }
            if (creatable) {
                AssistChip(
                    onClick = {
                        onCreate(query.trim())
                        query = ""
                    },
                    label = { Text(stringResource(Res.string.tags_create, query.trim())) },
                    leadingIcon = {
                        Icon(FeatherIcons.Plus, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
        }
    }
}

private val previewVocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
    TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
} + TastingTag(id = "orchid", key = null, name = "Orchid", isCustom = true, displayOrder = 23)

private val previewTags = listOf(
    SteepTag("floral", confirmed = true),
    SteepTag("orchid", confirmed = true),
    SteepTag("returning_sweetness", confirmed = false),
)

@Preview(showBackground = true)
@Composable
private fun SteepTagsPreview() {
    LeafLogTheme {
        SteepTags(
            tags = previewTags,
            vocabulary = previewVocabulary,
            onToggle = {},
            onCreate = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepTagChipsPreview() {
    LeafLogTheme {
        SteepTagChips(tags = previewTags, vocabulary = previewVocabulary, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TagChipsPreview() {
    LeafLogTheme {
        TagChips(tags = previewVocabulary.takeLast(3), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TagPickerPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            TagPickerContent(tags = previewTags, vocabulary = previewVocabulary, onToggle = {}, onCreate = {})
        }
    }
}
