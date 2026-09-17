package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Name-and-type form for creating a tea without leaving the tea picker.
 *
 * Rendered in place of the picker's body inside the same sheet, so it brings its own header and
 * action row rather than relying on a surrounding dialog.
 */
@Composable
internal fun QuickAddTeaForm(
    teaTypes: List<TeaType>,
    initialName: String,
    onCancel: () -> Unit,
    onSave: (name: String, teaTypeId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }
    // TextFieldValue rather than String so a prefilled name puts the cursor after it, not before.
    var name by remember {
        mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length)))
    }
    var selectedTeaTypeId by rememberSaveable(teaTypes) {
        mutableStateOf(teaTypes.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = FeatherIcons.ArrowLeft,
                    contentDescription = "Back to tea list",
                )
            }
            Text(
                text = "Add new tea",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tea Name *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(nameFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        Text(
            text = "Tea Type *",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            teaTypes.forEach { teaType ->
                FilterChip(
                    selected = teaType.id == selectedTeaTypeId,
                    onClick = { selectedTeaTypeId = teaType.id },
                    label = { Text(teaType.name) },
                )
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(name.text.trim(), selectedTeaTypeId) },
                enabled = name.text.isNotBlank() && selectedTeaTypeId.isNotBlank(),
            ) {
                Text("Add Tea")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddTeaFormPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            QuickAddTeaForm(
                teaTypes = listOf(
                    TeaType(id = "green", name = "Green", colorHex = "#4CAF50"),
                    TeaType(id = "black", name = "Black", colorHex = "#795548"),
                    TeaType(id = "oolong", name = "Oolong", colorHex = "#FF9800"),
                ),
                initialName = "",
                onCancel = {},
                onSave = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddTeaFormPrefilledPreview() {
    LeafLogTheme {
        SheetPreviewContainer {
            QuickAddTeaForm(
                teaTypes = listOf(
                    TeaType(id = "green", name = "Green", colorHex = "#4CAF50"),
                    TeaType(id = "black", name = "Black", colorHex = "#795548"),
                    TeaType(id = "oolong", name = "Oolong", colorHex = "#FF9800"),
                ),
                initialName = "Dragon Well",
                onCancel = {},
                onSave = { _, _ -> },
            )
        }
    }
}
