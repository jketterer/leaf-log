package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorChip
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.components.session.searchTags
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.tag_merge_into_title
import leaf_log.shared.generated.resources.tag_merge_search
import org.jetbrains.compose.resources.stringResource
import dev.jketterer.leaflog.presentation.ui.components.common.rememberExpandedSheetState

/** Every tag but [tag], to search and pick the one it merges into. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MergeTagSheet(
    tag: TastingTag,
    vocabulary: List<TastingTag>,
    onPick: (targetId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberExpandedSheetState()) {
        MergeTagContent(tag, vocabulary, onPick)
    }
}

@Composable
private fun MergeTagContent(
    tag: TastingTag,
    vocabulary: List<TastingTag>,
    onPick: (targetId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val (matches, _) = searchTags(vocabulary.filter { it.id != tag.id }.map { it to it.label() }, query)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.tag_merge_into_title, tag.label()),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(Res.string.tag_merge_search)) },
            leadingIcon = { Icon(FeatherIcons.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        ) {
            matches.forEach { (target, label) ->
                FlavorChip(label = label, isCustom = target.isCustom, onClick = { onPick(target.id) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MergeTagPreview() {
    val peachy = TastingTag(id = "peachy", key = null, name = "Peachy", isCustom = true, displayOrder = 23)
    val vocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
        TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
    } + peachy + TastingTag(id = "petrichor", key = null, name = "Petrichor", isCustom = true, displayOrder = 24)
    LeafLogTheme {
        SheetPreviewContainer {
            MergeTagContent(tag = peachy, vocabulary = vocabulary, onPick = {})
        }
    }
}
