package dev.jketterer.leaflog.presentation.ui.screens.settings.teatype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.DialogPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import kotlin.time.Instant

/** Deletes a type no tea uses; a type in use is merged into another instead. */
@Composable
internal fun DeleteTeaTypeDialog(
    teaTypeName: String,
    canDelete: Boolean,
    teaCount: Int,
    mergeTargets: List<TeaType>,
    mergeTargetId: String?,
    onMergeTargetSelected: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canDelete) "Delete Tea Type" else "Merge Tea Type") },
        text = {
            DeleteTeaTypeDialogContent(
                teaTypeName = teaTypeName,
                canDelete = canDelete,
                teaCount = teaCount,
                mergeTargets = mergeTargets,
                mergeTargetId = mergeTargetId,
                onMergeTargetSelected = onMergeTargetSelected,
            )
        },
        confirmButton = {
            if (canDelete) {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete")
                }
            } else {
                TextButton(onClick = onConfirmMerge, enabled = mergeTargetId != null) {
                    Text("Merge")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteTeaTypeDialogContent(
    teaTypeName: String,
    canDelete: Boolean,
    teaCount: Int,
    mergeTargets: List<TeaType>,
    mergeTargetId: String?,
    onMergeTargetSelected: (String) -> Unit,
) {
    if (canDelete) {
        Text("Are you sure you want to delete \"$teaTypeName\"?")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "\"$teaTypeName\" is used by $teaCount ${if (teaCount == 1) "tea" else "teas"}. " +
                "Choose a type to move them to, and \"$teaTypeName\" will be deleted.",
        )
        Column(
            modifier = Modifier
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            mergeTargets.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = type.id == mergeTargetId,
                            onClick = { onMergeTargetSelected(type.id) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = type.id == mergeTargetId, onClick = null)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(teaTypeColor(type.colorHex)),
                    )
                    Text(type.name)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteDialogCanDeletePreview() {
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Delete Tea Type",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                TextButton(onClick = {}) { Text("Delete") }
            },
        ) {
            DeleteTeaTypeDialogContent(
                teaTypeName = "Herbal",
                canDelete = true,
                teaCount = 0,
                mergeTargets = emptyList(),
                mergeTargetId = null,
                onMergeTargetSelected = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteDialogMergePreview() {
    val now = Instant.fromEpochMilliseconds(0)
    fun type(id: String, name: String, colorHex: String) = TeaType(
        id = id,
        name = name,
        colorHex = colorHex,
        isSystemDefault = true,
        displayOrder = 0,
        createdAt = now,
        updatedAt = now,
    )
    LeafLogTheme {
        DialogPreviewContainer(
            title = "Merge Tea Type",
            buttons = {
                TextButton(onClick = {}) { Text("Cancel") }
                TextButton(onClick = {}) { Text("Merge") }
            },
        ) {
            DeleteTeaTypeDialogContent(
                teaTypeName = "Green",
                canDelete = false,
                teaCount = 5,
                mergeTargets = listOf(
                    type("black", "Black", "#795548"),
                    type("oolong", "Oolong", "#FF9800"),
                    type("white", "White", "#FFFDE7"),
                ),
                mergeTargetId = "oolong",
                onMergeTargetSelected = {},
            )
        }
    }
}
