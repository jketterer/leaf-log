package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.presentation.ui.components.common.EditCapacityField
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoPickerButton
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIconHelper
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Title above a group of fields. */
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
 * What the vessel is called and what it looks like in a list.
 *
 * Headerless: the name is the only required field on the screen, and a header over it would make
 * it read as one optional group among several.
 */
@Composable
internal fun VesselIdentitySection(
    name: String,
    nameError: String?,
    selectedIconName: String,
    onNameChanged: (String) -> Unit,
    onIconSelected: (String) -> Unit,
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
            label = { Text("Name *") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNameImeNext() }),
        )

        VesselIconChips(
            selectedIconName = selectedIconName,
            onIconSelected = onIconSelected,
        )
    }
}

/**
 * Icon as a wrapping chip row.
 *
 * Not marked required: one is always pre-selected, so an asterisk would promise a choice the user
 * never has to make. The supporting line says what the icon is actually for - every list and detail
 * view shows the photo instead when there is one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VesselIconChips(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            // No vertical spacing: a chip's 48dp touch target is taller than the 32dp it draws, so
            // wrapped rows already sit further apart than the horizontal gap.
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            VesselIconHelper.getAllIcons().forEach { iconOption ->
                FilterChip(
                    selected = iconOption.iconName == selectedIconName,
                    onClick = { onIconSelected(iconOption.iconName) },
                    label = { Text(iconOption.displayName) },
                    leadingIcon = {
                        Icon(
                            painter = iconOption.painter(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
        Text(
            text = "Used when there's no photo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Everything optional about the vessel itself. */
@Composable
internal fun VesselDetailsSection(
    capacity: String,
    capacityError: String?,
    volumeUnit: VolumeUnit,
    onCapacityChanged: (String) -> Unit,
    onToggleVolumeUnit: () -> Unit,
    capacityFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Details")

        EditCapacityField(
            value = capacity,
            onValueChange = onCapacityChanged,
            isError = capacityError != null,
            supportingText = capacityError
                ?: "Optional - auto-fills water quantity when selected",
            enabled = true,
            volumeUnit = volumeUnit,
            onToggleUnit = onToggleVolumeUnit,
            modifier = Modifier.focusRequester(capacityFocusRequester),
        )
    }
}

@Composable
internal fun VesselPhotoSection(
    imagePath: String?,
    onPhotoPicked: (ByteArray) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    // Null in a preview, where no Koin application is running and resolving would throw. The
    // section then skips the image, which it has no storage to resolve a path against anyway.
    imageStorage: ImageStorage? = if (LocalInspectionMode.current) null else koinInject(),
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader("Photo")

        if (imagePath != null && imageStorage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    // saveImage returns a path relative to the app's files directory, so it has to
                    // be resolved before Coil can load it.
                    model = imageStorage.resolveImagePath(imagePath),
                    contentDescription = "Vessel photo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    PhotoPickerButton(
                        onPhotoPicked = { file ->
                            scope.launch { onPhotoPicked(file.readBytes()) }
                        },
                        text = "Change Photo",
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onRemovePhoto) {
                        Icon(
                            imageVector = FeatherIcons.X,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        } else {
            PhotoPickerButton(
                onPhotoPicked = { file ->
                    scope.launch { onPhotoPicked(file.readBytes()) }
                },
                text = "Add Photo",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselIdentitySectionPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VesselIdentitySection(
                name = "Yixing Gaiwan",
                nameError = null,
                selectedIconName = "gaiwan",
                onNameChanged = {},
                onIconSelected = {},
                nameFocusRequester = remember { FocusRequester() },
                onNameImeNext = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselIdentitySectionErrorPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VesselIdentitySection(
                name = "",
                nameError = "Name is required",
                selectedIconName = DEFAULT_ICON_NAME,
                onNameChanged = {},
                onIconSelected = {},
                nameFocusRequester = remember { FocusRequester() },
                onNameImeNext = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselDetailsSectionPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VesselDetailsSection(
                capacity = "150",
                capacityError = null,
                volumeUnit = VolumeUnit.MILLILITERS,
                onCapacityChanged = {},
                onToggleVolumeUnit = {},
                capacityFocusRequester = remember { FocusRequester() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselPhotoSectionEmptyPreview() {
    LeafLogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VesselPhotoSection(
                imagePath = null,
                onPhotoPicked = {},
                onRemovePhoto = {},
            )
        }
    }
}
