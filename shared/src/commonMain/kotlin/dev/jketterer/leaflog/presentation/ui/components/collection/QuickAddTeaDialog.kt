package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Dialog for creating a tea with only a name and type, without leaving the current flow.
 */
@Composable
fun QuickAddTeaDialog(
    teaTypes: List<TeaType>,
    onSave: (name: String, teaTypeId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedTeaTypeId by remember { mutableStateOf(teaTypes.firstOrNull()?.id ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Add New Tea") },
        text = {
            QuickAddTeaDialogContent(
                teaTypes = teaTypes,
                name = name,
                nameError = nameError,
                selectedTeaTypeId = selectedTeaTypeId,
                onNameChanged = {
                    name = it
                    nameError = if (it.isBlank()) "Name is required" else null
                },
                onTeaTypeSelected = { selectedTeaTypeId = it },
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name is required"
                    } else {
                        onSave(name.trim(), selectedTeaTypeId)
                    }
                },
                enabled = selectedTeaTypeId.isNotBlank(),
            ) {
                Text("Add Tea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTeaDialogContent(
    teaTypes: List<TeaType>,
    name: String,
    nameError: String?,
    selectedTeaTypeId: String,
    onNameChanged: (String) -> Unit,
    onTeaTypeSelected: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showTypeMenu by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Tea Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        ExposedDropdownMenuBox(
            expanded = showTypeMenu,
            onExpandedChange = { showTypeMenu = it },
        ) {
            OutlinedTextField(
                value = teaTypes.find { it.id == selectedTeaTypeId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tea Type *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTypeMenu) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = showTypeMenu,
                onDismissRequest = { showTypeMenu = false },
            ) {
                teaTypes.forEach { teaType ->
                    DropdownMenuItem(
                        text = { Text(teaType.name) },
                        onClick = {
                            onTeaTypeSelected(teaType.id)
                            showTypeMenu = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddTeaDialogPreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Add New Tea",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                Button(onClick = {}) { Text("Add Tea") }
            },
        ) {
            QuickAddTeaDialogContent(
                teaTypes = listOf(
                    TeaType(id = "green", name = "Green", colorHex = "#4CAF50"),
                    TeaType(id = "black", name = "Black", colorHex = "#795548"),
                ),
                name = "Dragon Well",
                nameError = null,
                selectedTeaTypeId = "green",
                onNameChanged = {},
                onTeaTypeSelected = {},
            )
        }
    }
}
