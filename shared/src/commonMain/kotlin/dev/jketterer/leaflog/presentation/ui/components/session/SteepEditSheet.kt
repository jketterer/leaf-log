package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.Trash2
import dev.jketterer.leaflog.domain.models.Steep
import dev.jketterer.leaflog.domain.models.SteepTag
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.common.BrewParametersContent
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.delete_steep
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.review_add_note
import leaf_log.shared.generated.resources.review_note_label
import leaf_log.shared.generated.resources.review_rate_steep
import leaf_log.shared.generated.resources.review_steep_n
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One steep's [SteepEditBody] in a sheet. [rating] and [notes] are the caller's edits in progress;
 * the parameters are edited in place of the rest and reported on Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteepEditSheet(
    steep: Steep,
    rating: Float,
    notes: String,
    vocabulary: List<TastingTag>,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagCreate: (String) -> Unit,
    onParametersChange: (brewedTime: Duration, temperatureCelsius: Double, waterQuantityMl: Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Offered beside the steep number when given
    onDelete: (() -> Unit)? = null,
) {
    var editingParameters by remember(steep.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        // Must live inside the sheet's own window, or the sheet consumes back before this sees it.
        // Back returns to the steep; swipe-down and scrim tap still close the sheet outright.
        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = backState,
            isBackEnabled = editingParameters,
            onBackCompleted = { editingParameters = false },
        )

        AnimatedContent(
            targetState = editingParameters,
            transitionSpec = { sharedAxisX(forward = targetState) },
        ) { editing ->
            if (editing) {
                SteepParametersContent(
                    steep = steep,
                    onSave = { time, temperature, water ->
                        onParametersChange(time, temperature, water)
                        editingParameters = false
                    },
                    onCancel = { editingParameters = false },
                )
            } else {
                SteepEditSheetContent(
                    steep = steep,
                    rating = rating,
                    notes = notes,
                    vocabulary = vocabulary,
                    onRatingChange = onRatingChange,
                    onNotesChange = onNotesChange,
                    onTagToggle = onTagToggle,
                    onTagCreate = onTagCreate,
                    onEditParameters = { editingParameters = true },
                    onDelete = onDelete,
                )
            }
        }
    }
}

/** Material's shared axis X: a short slide with a fade, toward the content being opened. */
private fun sharedAxisX(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (fadeIn(tween(210, delayMillis = 90)) +
        slideInHorizontally(tween(300)) { direction * it / 10 }) togetherWith
        (fadeOut(tween(90)) + slideOutHorizontally(tween(300)) { -direction * it / 10 })
}

@Composable
private fun SteepParametersContent(
    steep: Steep,
    onSave: (Duration, Double, Double) -> Unit,
    onCancel: () -> Unit,
) {
    var brewedTime by remember(steep.id) { mutableStateOf(steep.brewedTime) }
    var temperature by remember(steep.id) { mutableStateOf<Double?>(steep.temperatureCelsius) }
    var water by remember(steep.id) { mutableStateOf<Double?>(steep.waterQuantityMl) }

    BrewParametersContent(
        brewingTime = brewedTime,
        onBrewingTimeChanged = { brewedTime = it },
        temperatureCelsius = temperature,
        onTemperatureChanged = { temperature = it },
        waterQuantityMl = water,
        onWaterQuantityChanged = { water = it },
        onSave = {
            val t = temperature
            val w = water
            if (t != null && w != null) onSave(brewedTime, t, w)
        },
        onDismiss = onCancel,
    )
}

@Composable
private fun SteepEditSheetContent(
    steep: Steep,
    rating: Float,
    notes: String,
    vocabulary: List<TastingTag>,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagCreate: (String) -> Unit,
    onEditParameters: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.review_steep_n, steep.number),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(FeatherIcons.Trash2, contentDescription = stringResource(Res.string.delete_steep))
                }
            }
        }
        SteepEditBody(
            steep = steep,
            rating = rating,
            notes = notes,
            vocabulary = vocabulary,
            onRatingChange = onRatingChange,
            onNotesChange = onNotesChange,
            onTagToggle = onTagToggle,
            onTagCreate = onTagCreate,
            onEditParameters = onEditParameters,
        )
    }
}

/**
 * One steep's rating, parameters, tags and note, edited the same way whether it was just brewed or
 * long finished. The note stays folded away until asked for, or already written.
 */
@Composable
fun SteepEditBody(
    steep: Steep,
    rating: Float,
    notes: String,
    vocabulary: List<TastingTag>,
    onRatingChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagCreate: (String) -> Unit,
    onEditParameters: () -> Unit,
    modifier: Modifier = Modifier,
    // Shown among the parameters when given, for a steep whose edit sheet changes the leaf
    teaQuantityGrams: Float? = null,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.review_rate_steep),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RatingSelector(
            rating = rating,
            onRatingChange = onRatingChange,
            modifier = Modifier.fillMaxWidth(),
        )
        SteepParametersRow(steep, teaQuantityGrams, onClick = onEditParameters)
        SteepTags(
            tags = steep.tags,
            vocabulary = vocabulary,
            onToggle = onTagToggle,
            onCreate = onTagCreate,
            modifier = Modifier.fillMaxWidth(),
        )
        var noteOpen by remember(steep.id) { mutableStateOf(notes.isNotEmpty()) }
        if (noteOpen) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(Res.string.review_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        } else {
            TextButton(onClick = { noteOpen = true }, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(Res.string.review_add_note))
            }
        }
    }
}

@Composable
private fun SteepParametersRow(steep: Steep, teaQuantityGrams: Float?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(steep.brewedTime.inWholeSeconds.toInt()))
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(steep.temperatureCelsius))
                BrewingParamChip(FeatherIcons.Droplet, formatVolume(steep.waterQuantityMl))
                teaQuantityGrams?.let {
                    BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it))
                }
            }
            Icon(
                imageVector = FeatherIcons.Edit2,
                contentDescription = stringResource(Res.string.edit_parameters),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepEditSheetPreview() {
    val steep = previewSession(ratings = listOf(4f), brewingTime = 25.seconds).steeps.first()
    LeafLogTheme {
        SheetPreviewContainer {
            SteepEditSheetContent(
                steep = steep.copy(tags = listOf(SteepTag("floral", confirmed = true))),
                rating = 4f,
                notes = "Sweet finish",
                vocabulary = listOf(
                    TastingTag(
                        "floral",
                        key = "floral",
                        name = null,
                        isCustom = false,
                        displayOrder = 0,
                    ),
                ),
                onRatingChange = {},
                onNotesChange = {},
                onTagToggle = {},
                onTagCreate = {},
                onEditParameters = {},
                onDelete = {},
            )
        }
    }
}
