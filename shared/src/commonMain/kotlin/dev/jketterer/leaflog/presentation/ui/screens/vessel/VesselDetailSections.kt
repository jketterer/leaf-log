package dev.jketterer.leaflog.presentation.ui.screens.vessel

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.Archive
import compose.icons.feathericons.Droplet
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.VesselTea
import dev.jketterer.leaflog.domain.models.formatRelativeTimestamp
import dev.jketterer.leaflog.presentation.ui.components.analytics.SummaryCard
import dev.jketterer.leaflog.presentation.ui.components.collection.TeaRow
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.components.common.formatVolume
import dev.jketterer.leaflog.presentation.ui.components.session.FlavorRow
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselIconHelper
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.header_seasoned_with
import leaf_log.shared.generated.resources.sessions_label
import leaf_log.shared.generated.resources.teas_label
import leaf_log.shared.generated.resources.vessel_archived_label
import leaf_log.shared.generated.resources.vessel_last_brewed
import leaf_log.shared.generated.resources.vessel_mostly
import leaf_log.shared.generated.resources.vessel_only
import leaf_log.shared.generated.resources.vessel_photo
import leaf_log.shared.generated.resources.header_teas
import leaf_log.shared.generated.resources.show_all_teas
import leaf_log.shared.generated.resources.vessel_tea_sessions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** The vessel's photo, full width, opening full screen on a tap. */
@Composable
internal fun VesselPhoto(
    photo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = photo,
        contentDescription = stringResource(Res.string.vessel_photo),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
    )
}

/**
 * The vessel's name, what it's kept for and its capacity. Without a photo above it, its icon sits
 * beside them.
 */
@Composable
internal fun VesselHeading(
    vessel: BrewingVessel,
    dominantType: TeaType?,
    onlyType: Boolean,
    showIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = VesselIconHelper.getIconForVessel(vessel.iconName),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = vessel.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    vessel.isArchived -> IconLabel(FeatherIcons.Archive, stringResource(Res.string.vessel_archived_label))
                    dominantType != null -> DominantTypeChip(dominantType, onlyType)
                }
                vessel.capacityMl?.let { IconLabel(FeatherIcons.Droplet, formatVolume(it.toDouble())) }
            }
        }
    }
}

@Composable
private fun DominantTypeChip(teaType: TeaType, onlyType: Boolean) {
    val color = teaTypeColor(teaType.colorHex).takeIf { it.isSpecified } ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(
                text = stringResource(if (onlyType) Res.string.vessel_only else Res.string.vessel_mostly, teaType.name),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Sessions, teas and when it was last brewed in, a tile each. */
@Composable
internal fun VesselStats(
    sessions: Int,
    teas: Int,
    lastBrewed: Instant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            value = sessions.toString(),
            label = pluralStringResource(Res.plurals.sessions_label, sessions),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            value = teas.toString(),
            label = pluralStringResource(Res.plurals.teas_label, teas),
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            value = TimeFormatter.formatRelativeTimestamp(lastBrewed, currentInstant()),
            label = stringResource(Res.string.vessel_last_brewed),
            modifier = Modifier.weight(1f),
        )
    }
}

/** The tea types brewed in it, a bar each in the type's color. */
@Composable
internal fun SeasonedWithSection(
    types: List<TeaTypeDistribution>,
    sessions: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(stringResource(Res.string.header_seasoned_with))
        types.forEach { type ->
            FlavorRow(
                label = type.teaType.name,
                count = type.sessionCount,
                outOf = sessions,
                color = teaTypeColor(type.teaType.colorHex).takeIf { it.isSpecified }
                    ?: MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/** Teas shown before "Show all". */
private const val TEAS_SHOWN = 5

/**
 * The teas brewed in the vessel, a row each with how often and how lately it was brewed in this
 * one. Past [TEAS_SHOWN], the rest wait behind "Show all". Its rows run edge to edge, so it insets
 * only its header and button.
 */
@Composable
internal fun VesselTeasSection(
    teas: List<VesselTea>,
    onTeaClick: (teaId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val now = currentInstant()
    Column(modifier = modifier.animateContentSize()) {
        SectionHeader(
            stringResource(Res.string.header_teas),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )
        (if (showAll) teas else teas.take(TEAS_SHOWN)).forEach { tea ->
            TeaRow(
                tea = tea.tea,
                teaTypeColorHex = tea.teaType?.colorHex,
                supportingText = pluralStringResource(
                    Res.plurals.vessel_tea_sessions,
                    tea.sessions,
                    tea.sessions,
                    TimeFormatter.formatRelativeTimestamp(tea.lastBrewed, now),
                ),
                onClick = { onTeaClick(tea.tea.id) },
            )
        }
        if (!showAll && teas.size > TEAS_SHOWN) {
            TextButton(onClick = { showAll = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(pluralStringResource(Res.plurals.show_all_teas, teas.size, teas.size))
            }
        }
    }
}

internal val previewSheng = TeaType(id = "sheng", name = "Sheng", colorHex = "#8BC34A")
internal val previewShou = TeaType(id = "shou", name = "Shou", colorHex = "#5D4037")

internal fun previewVessel(
    imagePath: String? = null,
    isArchived: Boolean = false,
) = BrewingVessel(
    id = "pot",
    name = "Duanni Shui Ping",
    iconName = "teapot",
    imagePath = imagePath,
    capacityMl = 120,
    isSystemDefault = false,
    isArchived = isArchived,
    displayOrder = 0,
    createdAt = PreviewInstant,
    updatedAt = PreviewInstant,
)

internal fun previewVesselTea(
    name: String,
    teaType: TeaType,
    sessions: Int,
    daysAgo: Int,
    finished: Boolean = false,
) = VesselTea(
    tea = Tea(
        id = name,
        name = name,
        teaTypeId = teaType.id,
        finishedAt = if (finished) PreviewInstant else null,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
    teaType = teaType,
    sessions = sessions,
    lastBrewed = PreviewInstant - daysAgo.days,
)

@Preview(showBackground = true)
@Composable
private fun VesselHeadingPreview() {
    LeafLogTheme {
        VesselHeading(
            vessel = previewVessel(),
            dominantType = previewSheng,
            onlyType = false,
            showIcon = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselHeadingOnlyTypePreview() {
    LeafLogTheme {
        VesselHeading(
            vessel = previewVessel(),
            dominantType = previewSheng,
            onlyType = true,
            showIcon = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselHeadingArchivedPreview() {
    LeafLogTheme {
        VesselHeading(
            vessel = previewVessel(isArchived = true),
            dominantType = previewSheng,
            onlyType = false,
            showIcon = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselStatsPreview() {
    LeafLogTheme {
        VesselStats(sessions = 48, teas = 9, lastBrewed = PreviewInstant - 3.days, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SeasonedWithSectionPreview() {
    LeafLogTheme {
        SeasonedWithSection(
            types = listOf(
                TeaTypeDistribution(previewSheng, 40, 83f),
                TeaTypeDistribution(previewShou, 8, 17f),
            ),
            sessions = 48,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VesselTeasSectionPreview() {
    LeafLogTheme {
        VesselTeasSection(
            teas = listOf(
                previewVesselTea("2019 Yiwu Gushu", previewSheng, 14, 3),
                previewVesselTea("2008 Menghai 7542", previewShou, 8, 14),
                previewVesselTea("Bulang Old Tree", previewSheng, 9, 21),
                previewVesselTea("Jingmai Spring", previewSheng, 7, 30),
                previewVesselTea("Mengku Bingdao", previewSheng, 6, 45).copy(teaType = null),
                previewVesselTea("Old Lao Ban Zhang", previewSheng, 1, 90, finished = true),
            ),
            onTeaClick = {},
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
