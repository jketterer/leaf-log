package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import dev.jketterer.leaflog.domain.models.BrewingConfiguration
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.screens.home.QuickBrewCardData
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQuickBrewSheet(
    configurations: List<QuickBrewCardData>,
    onPinToggled: (configurationId: String, isPinned: Boolean) -> Unit,
    onReorder: (orderedIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ManageQuickBrewSheetContent(
            configurations = configurations,
            onPinToggled = onPinToggled,
            onReorder = onReorder,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ManageQuickBrewSheetContent(
    configurations: List<QuickBrewCardData>,
    onPinToggled: (configurationId: String, isPinned: Boolean) -> Unit,
    onReorder: (orderedIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val pinned = configurations.filter { it.configuration.isPinned }
        .sortedBy { it.configuration.pinnedSortOrder }
    val unpinned = configurations.filter { !it.configuration.isPinned }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Manage Quick Brew",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Pin and reorder your brewing methods",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pinned section
        Text(
            text = "PINNED",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (pinned.isEmpty()) {
            Text(
                text = "No pinned methods yet. Tap the bookmark icon to pin a method.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pinned.forEachIndexed { index, data ->
                    ManageConfigRow(
                        data = data,
                        isPinned = true,
                        isFirst = index == 0,
                        isLast = index == pinned.lastIndex,
                        onPin = { onPinToggled(data.configuration.id, true) },
                        onUnpin = { onPinToggled(data.configuration.id, false) },
                        onMoveUp = {
                            val ids = pinned.map { it.configuration.id }.toMutableList()
                            val tmp = ids[index]
                            ids[index] = ids[index - 1]
                            ids[index - 1] = tmp
                            onReorder(ids)
                        },
                        onMoveDown = {
                            val ids = pinned.map { it.configuration.id }.toMutableList()
                            val tmp = ids[index]
                            ids[index] = ids[index + 1]
                            ids[index + 1] = tmp
                            onReorder(ids)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // By usage section
        if (unpinned.isNotEmpty()) {
            Text(
                text = "BY USAGE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                unpinned.forEach { data ->
                    ManageConfigRow(
                        data = data,
                        isPinned = false,
                        isFirst = false,
                        isLast = false,
                        onPin = { onPinToggled(data.configuration.id, true) },
                        onUnpin = { onPinToggled(data.configuration.id, false) },
                        onMoveUp = {},
                        onMoveDown = {},
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun ManageConfigRow(
    data: QuickBrewCardData,
    isPinned: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val accentColor = data.teaTypeColorHex?.hexToColor()
        ?: MaterialTheme.colorScheme.primaryContainer

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored accent bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(accentColor),
            )

            // Config info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = data.configuration.label ?: "Unnamed Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = data.teaName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                if (isPinned) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.ChevronUp,
                            contentDescription = "Move up",
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.ChevronDown,
                            contentDescription = "Move down",
                        )
                    }
                    IconButton(onClick = onUnpin) {
                        Icon(
                            imageVector = FeatherIcons.Bookmark,
                            contentDescription = "Unpin",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    if (data.configuration.timesUsed > 0) {
                        Text(
                            text = "${data.configuration.timesUsed}×",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    IconButton(onClick = onPin) {
                        Icon(
                            imageVector = FeatherIcons.Bookmark,
                            contentDescription = "Pin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ManageQuickBrewSheetWithPinnedPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        ManageQuickBrewSheetContent(
            configurations = listOf(
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "1",
                        teaId = "tea1",
                        vesselId = "v1",
                        teaQuantityGrams = 5f,
                        waterQuantityMl = 100.0,
                        temperatureCelsius = 95.0,
                        brewingTime = 30.seconds,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Gaiwan · Gong-fu",
                        isActive = true,
                        timesUsed = 24,
                        isPinned = true,
                        pinnedSortOrder = 0,
                        lastUsedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    teaName = "Tie Guan Yin",
                    teaTypeName = "Oolong",
                    teaTypeColorHex = "#FF9800",
                    vesselName = "Gaiwan",
                ),
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "2",
                        teaId = "tea2",
                        vesselId = "v2",
                        teaQuantityGrams = 3f,
                        waterQuantityMl = 200.0,
                        temperatureCelsius = 100.0,
                        brewingTime = 4.minutes,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Quick Black",
                        isActive = true,
                        timesUsed = 10,
                        isPinned = true,
                        pinnedSortOrder = 1,
                        lastUsedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    teaName = "English Breakfast",
                    teaTypeName = "Black",
                    teaTypeColorHex = "#795548",
                    vesselName = "Mug",
                ),
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "3",
                        teaId = "tea3",
                        vesselId = "v3",
                        teaQuantityGrams = 2f,
                        waterQuantityMl = 350.0,
                        temperatureCelsius = 80.0,
                        brewingTime = 3.minutes,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Mug · Western",
                        isActive = true,
                        timesUsed = 8,
                        isPinned = false,
                        pinnedSortOrder = 0,
                        lastUsedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    teaName = "Sencha",
                    teaTypeName = "Green",
                    teaTypeColorHex = "#4CAF50",
                    vesselName = "Mug",
                ),
            ),
            onPinToggled = { _, _ -> },
            onReorder = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManageQuickBrewSheetNoPinnedPreview() {
    val now = Instant.fromEpochMilliseconds(0)
    LeafLogTheme {
        ManageQuickBrewSheetContent(
            configurations = listOf(
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "1",
                        teaId = "tea1",
                        vesselId = "v1",
                        teaQuantityGrams = 5f,
                        waterQuantityMl = 100.0,
                        temperatureCelsius = 95.0,
                        brewingTime = 30.seconds,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Gaiwan · Gong-fu",
                        isActive = true,
                        timesUsed = 24,
                        isPinned = false,
                        pinnedSortOrder = 0,
                        lastUsedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    teaName = "Tie Guan Yin",
                    teaTypeName = "Oolong",
                    teaTypeColorHex = "#FF9800",
                    vesselName = "Gaiwan",
                ),
                QuickBrewCardData(
                    configuration = BrewingConfiguration(
                        id = "2",
                        teaId = "tea2",
                        vesselId = "v2",
                        teaQuantityGrams = 2f,
                        waterQuantityMl = 350.0,
                        temperatureCelsius = 80.0,
                        brewingTime = 3.minutes,
                        waterType = WaterType.FILTERED,
                        sourceSessionId = null,
                        rating = null,
                        label = "Mug · Western",
                        isActive = true,
                        timesUsed = 8,
                        isPinned = false,
                        pinnedSortOrder = 0,
                        lastUsedAt = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    teaName = "Sencha",
                    teaTypeName = "Green",
                    teaTypeColorHex = "#4CAF50",
                    vesselName = "Mug",
                ),
            ),
            onPinToggled = { _, _ -> },
            onReorder = {},
            onDismiss = {},
        )
    }
}
