package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.koinInject

/** The vessels as a row of chips, each with its picture; one tap picks it. Scrolls once they don't fit. */
@Composable
fun VesselChipRow(
    vessels: List<BrewingVessel>,
    selectedVessel: BrewingVessel?,
    onVesselSelected: (BrewingVessel) -> Unit,
    modifier: Modifier = Modifier,
    // Null in a preview, where no Koin application is running and resolving would throw.
    imageStorage: ImageStorage? = if (LocalInspectionMode.current) null else koinInject(),
) {
    val listState = rememberLazyListState()
    val selectedIndex = vessels.indexOfFirst { it.id == selectedVessel?.id }

    // A vessel chosen elsewhere (a past brew, a restored form) may sit past the edge
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val layout = listState.layoutInfo
        val shown = layout.visibleItemsInfo.any {
            it.index == selectedIndex && it.offset >= 0 && it.offset + it.size <= layout.viewportEndOffset
        }
        if (!shown) listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(vessels, key = { _, vessel -> vessel.id }) { index, vessel ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onVesselSelected(vessel) },
                label = { Text(vessel.name) },
                leadingIcon = {
                    if (vessel.imagePath != null && imageStorage != null) {
                        AsyncImage(
                            model = imageStorage.resolveImagePath(vessel.imagePath),
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            painter = VesselIconHelper.getIconForVessel(vessel.iconName),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselChipRowPreview() {
    val vessels = listOf("gaiwan", "kyusu", "mug", "teapot").mapIndexed { index, id ->
        BrewingVessel(
            id = id,
            name = id.replaceFirstChar { it.uppercase() },
            iconName = id,
            isSystemDefault = true,
            displayOrder = index,
            createdAt = PreviewInstant,
            updatedAt = PreviewInstant,
        )
    }
    LeafLogTheme {
        VesselChipRow(
            vessels = vessels,
            selectedVessel = vessels.first(),
            onVesselSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
