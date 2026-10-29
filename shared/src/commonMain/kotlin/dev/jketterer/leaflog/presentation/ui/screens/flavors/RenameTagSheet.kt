package dev.jketterer.leaflog.presentation.ui.screens.flavors

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.session.label
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.tag_merge_instead
import leaf_log.shared.generated.resources.tag_name
import leaf_log.shared.generated.resources.tag_name_taken
import leaf_log.shared.generated.resources.tag_rename_title
import org.jetbrains.compose.resources.stringResource
import dev.jketterer.leaflog.presentation.ui.components.common.rememberExpandedSheetState

/**
 * Renames [tag]. A name another tag already has can't be saved; the sheet offers to merge into
 * that tag instead. A change of case alone is a rename.
 */
// A sheet rather than a dialog: a text field in an AlertDialog never lets Robolectric idle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RenameTagSheet(
    tag: TastingTag,
    vocabulary: List<TastingTag>,
    enabled: Boolean,
    onRename: (name: String) -> Unit,
    onMergeInstead: (targetId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberExpandedSheetState()) {
        RenameTagContent(tag, vocabulary, enabled, onRename, onMergeInstead)
    }
}

@Composable
private fun RenameTagContent(
    tag: TastingTag,
    vocabulary: List<TastingTag>,
    enabled: Boolean,
    onRename: (name: String) -> Unit,
    onMergeInstead: (targetId: String) -> Unit,
    modifier: Modifier = Modifier,
    initial: String = tag.label(),
) {
    val current = tag.label()
    var field by remember(tag.id) { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val word = field.text.trim()
    val taken = vocabulary.filter { it.id != tag.id }
        .map { it to it.label() }
        .find { it.second.equals(word, ignoreCase = true) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.tag_rename_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            label = { Text(stringResource(Res.string.tag_name)) },
            singleLine = true,
            isError = taken != null,
            supportingText = taken?.let { { Text(stringResource(Res.string.tag_name_taken, it.second)) } },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        AnimatedContent(targetState = taken?.first, modifier = Modifier.align(Alignment.End)) { target ->
            if (target != null) {
                Button(onClick = { onMergeInstead(target.id) }, enabled = enabled) {
                    Text(stringResource(Res.string.tag_merge_instead))
                }
            } else {
                Button(onClick = { onRename(word) }, enabled = enabled && word.isNotEmpty() && word != current) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RenameTagTakenPreview() {
    val peachy = TastingTag(id = "peachy", key = null, name = "Peachy", isCustom = true, displayOrder = 23)
    val vocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
        TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
    } + peachy
    LeafLogTheme {
        SheetPreviewContainer(height = 280.dp) {
            RenameTagContent(peachy, vocabulary, enabled = true, onRename = {}, onMergeInstead = {}, initial = "fruity")
        }
    }
}
