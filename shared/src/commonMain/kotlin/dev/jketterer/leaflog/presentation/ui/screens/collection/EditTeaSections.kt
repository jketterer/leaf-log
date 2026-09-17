package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.components.common.DatePickerField
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.TemperatureInputField
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Title above a group of fields. Matches the existing Photos heading. */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

/**
 * The two fields a tea cannot be saved without.
 *
 * Deliberately headerless; these are the point of the screen, and a header would only make them
 * look like one optional group among several.
 */
@Composable
internal fun TeaIdentitySection(
    name: String,
    nameError: String?,
    teaTypes: List<TeaType>,
    selectedTeaTypeId: String?,
    teaTypeError: String?,
    onNameChanged: (String) -> Unit,
    onTeaTypeSelected: (String) -> Unit,
    nameFocusRequester: FocusRequester,
    onNameImeNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Tea Name *") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNameImeNext() }),
        )

        TeaTypeChips(
            teaTypes = teaTypes,
            selectedTeaTypeId = selectedTeaTypeId,
            teaTypeError = teaTypeError,
            onTeaTypeSelected = onTeaTypeSelected,
        )
    }
}

/**
 * Tea type as a wrapping chip row.
 *
 * FlowRow rather than a horizontally scrolling row: tea types are user-extensible, and this is the
 * screen where they get extended.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TeaTypeChips(
    teaTypes: List<TeaType>,
    selectedTeaTypeId: String?,
    teaTypeError: String?,
    onTeaTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Tea Type *",
            style = MaterialTheme.typography.labelMedium,
            color = if (teaTypeError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            // No vertical spacing: a chip's 48dp touch target is taller than the 32dp it draws, so
            // wrapped rows already sit further apart than the horizontal gap.
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            teaTypes.forEach { teaType ->
                FilterChip(
                    selected = teaType.id == selectedTeaTypeId,
                    onClick = { onTeaTypeSelected(teaType.id) },
                    label = { Text(teaType.name) },
                )
            }
        }
        if (teaTypeError != null) {
            Text(
                text = teaTypeError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Everything optional: provenance, notes, and the default brewing temperature. */
@Composable
internal fun TeaDetailsSection(
    origin: String,
    producer: String,
    availableProducers: List<String>,
    purchaseDate: LocalDate?,
    description: String,
    temperatureDisplay: String,
    temperatureUnit: TemperatureUnit,
    temperatureError: String?,
    onOriginChanged: (String) -> Unit,
    onProducerChanged: (String) -> Unit,
    onPurchaseDateChanged: (LocalDate?) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onToggleTemperatureUnit: () -> Unit,
    originFocusRequester: FocusRequester,
    producerFocusRequester: FocusRequester,
    descriptionFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Details")

        OutlinedTextField(
            value = origin,
            onValueChange = onOriginChanged,
            label = { Text("Origin") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(originFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { producerFocusRequester.requestFocus() }),
        )

        ProducerField(
            producer = producer,
            availableProducers = availableProducers,
            onProducerChanged = onProducerChanged,
            focusRequester = producerFocusRequester,
            onImeNext = { descriptionFocusRequester.requestFocus() },
        )

        DatePickerField(
            date = purchaseDate,
            onDateChange = onPurchaseDateChanged,
            label = "Purchased",
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(descriptionFocusRequester),
            minLines = 3,
            maxLines = 5,
            // No Done action: this is the one multi-line field, so Enter has to insert a newline
            // rather than dismiss the keyboard.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )

        TemperatureInputField(
            value = temperatureDisplay,
            onValueChange = onTemperatureChanged,
            currentUnit = temperatureUnit,
            onToggleUnit = onToggleTemperatureUnit,
            label = { Text("Default Temperature") },
            isError = temperatureError != null,
            supportingText = temperatureError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Producer field with autocomplete over the producers already in the collection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProducerField(
    producer: String,
    availableProducers: List<String>,
    onProducerChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    onImeNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val suggestions = remember(producer, availableProducers) {
        // Suppressed while blank: opening the full list the instant the field is tapped buries the
        // next field under a menu the user never asked for.
        if (producer.isBlank()) {
            emptyList()
        } else {
            availableProducers.filter { it.contains(producer, ignoreCase = true) && it != producer }
        }
    }
    val expanded = showMenu && suggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { showMenu = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = producer,
            onValueChange = {
                onProducerChanged(it)
                showMenu = true
            },
            label = { Text("Producer/Brand") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .focusRequester(focusRequester)
                // Losing focus has to close the menu, or it hangs over whichever field comes next.
                .onFocusChanged { if (!it.isFocused) showMenu = false },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    showMenu = false
                    onImeNext()
                },
            ),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { showMenu = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onProducerChanged(suggestion)
                        showMenu = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun TeaPhotosSection(
    photos: List<String>,
    onAddPhoto: (ByteArray) -> Unit,
    onRemovePhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader("Photos")
        PhotoGrid(
            photos = photos,
            onAddPhoto = onAddPhoto,
            onRemovePhoto = onRemovePhoto,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaIdentitySectionPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TeaIdentitySection(
                name = "Dragon Well",
                nameError = null,
                teaTypes = previewTeaTypes,
                selectedTeaTypeId = "green",
                teaTypeError = null,
                onNameChanged = {},
                onTeaTypeSelected = {},
                nameFocusRequester = remember { FocusRequester() },
                onNameImeNext = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaIdentitySectionErrorPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TeaIdentitySection(
                name = "",
                nameError = "Name is required",
                teaTypes = previewTeaTypes,
                selectedTeaTypeId = null,
                teaTypeError = "Tea type is required",
                onNameChanged = {},
                onTeaTypeSelected = {},
                nameFocusRequester = remember { FocusRequester() },
                onNameImeNext = {},
            )
        }
    }
}

private val previewTeaTypes = listOf(
    TeaType(
        id = "green",
        name = "Green",
        defaultTemperatureCelsius = 75.0,
        colorHex = "#4CAF50",
        isSystemDefault = true,
        displayOrder = 0,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
    TeaType(
        id = "black",
        name = "Black",
        defaultTemperatureCelsius = 95.0,
        colorHex = "#795548",
        isSystemDefault = true,
        displayOrder = 1,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
    TeaType(
        id = "oolong",
        name = "Oolong",
        defaultTemperatureCelsius = 90.0,
        colorHex = "#FF9800",
        isSystemDefault = true,
        displayOrder = 2,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
    TeaType(
        id = "puerh",
        name = "Pu-erh",
        defaultTemperatureCelsius = 100.0,
        colorHex = "#5D4037",
        isSystemDefault = true,
        displayOrder = 3,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
)
