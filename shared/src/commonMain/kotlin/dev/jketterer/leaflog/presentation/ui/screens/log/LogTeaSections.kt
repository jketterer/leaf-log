package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Play
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SessionWithSteeps
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.models.WeightUnit
import dev.jketterer.leaflog.presentation.ui.components.session.TeaHeader
import dev.jketterer.leaflog.presentation.ui.components.vessel.VesselChipRow
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import androidx.compose.ui.graphics.isSpecified
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.LeafQuantityField
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.RinseSwitch
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.WaterTypeSelector
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.TagChips
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.begin_session
import leaf_log.shared.generated.resources.change
import leaf_log.shared.generated.resources.collapse_optional_details
import leaf_log.shared.generated.resources.expand_optional_details
import leaf_log.shared.generated.resources.field_location_optional
import leaf_log.shared.generated.resources.log_tea_choose_body
import leaf_log.shared.generated.resources.log_tea_choose_title
import leaf_log.shared.generated.resources.prefill_tea_default
import leaf_log.shared.generated.resources.prefill_type_default
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.components.common.SectionDivider
import leaf_log.shared.generated.resources.best_brew
import leaf_log.shared.generated.resources.header_brewing_parameters
import leaf_log.shared.generated.resources.header_optional_details
import leaf_log.shared.generated.resources.last_time
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.past_brew
import leaf_log.shared.generated.resources.select_tea
import leaf_log.shared.generated.resources.scratch
import leaf_log.shared.generated.resources.steep_count
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import compose.icons.feathericons.Layers
import compose.icons.feathericons.TrendingUp
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatSteepIncrement
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.StringResource

/**
 * Where the form starts from: the pair's last or best brew, the past brew Log Tea was opened on, or
 * scratch. A chosen brew shows what the fields don't, and opens its session when tapped.
 */
@Composable
internal fun SourceBrewSection(
    source: SessionWithSteeps?,
    tags: List<TastingTag>,
    lastBrew: SessionWithSteeps?,
    bestBrew: SessionWithSteeps?,
    onSourceSelected: (SessionWithSteeps?) -> Unit,
    onSourceClicked: (SessionWithSteeps) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brews = listOfNotNull(
        lastBrew?.let { it to Res.string.last_time },
        bestBrew?.takeIf { it.session.id != lastBrew?.session?.id }?.let { it to Res.string.best_brew },
    )
    val choices: List<Pair<SessionWithSteeps?, StringResource>> =
        if (source == null || brews.any { it.first.session.id == source.session.id }) {
            brews
        } else {
            brews + (source to Res.string.past_brew)
        } + (null to Res.string.scratch)

    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, (brew, label) ->
                SegmentedButton(
                    selected = brew?.session?.id == source?.session?.id,
                    onClick = { onSourceSelected(brew) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                    // Up to four segments: a check beside the label crowds it, and the fill marks it
                    icon = {},
                ) {
                    Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        source?.let { SourceBrewSummary(it, tags, onClick = { onSourceClicked(it) }) }
    }
}

/** A past brew in the few lines a form above its own fields needs: when, how it went, its steeps. */
@Composable
private fun SourceBrewSummary(
    brew: SessionWithSteeps,
    tags: List<TastingTag>,
    onClick: () -> Unit,
) {
    val plan = remember(brew) { BrewingPlan.learnedFrom(brew.steeps) }
    val date = brew.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val note = brew.session.notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = TimeFormatter.formatDate(date, currentDate()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RatingDisplay(brew.effectiveRating)
            plan.increment?.let { step ->
                BrewingParamChip(FeatherIcons.TrendingUp, formatSteepIncrement(step))
                val steeps = brew.steeps.size
                BrewingParamChip(FeatherIcons.Layers, pluralStringResource(Res.plurals.steep_count, steeps, steeps))
            }
        }
        if (tags.isNotEmpty()) TagChips(tags)
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The tea and vessel being brewed, named as the brew screens name them, over the vessels to pick
 * from. With no tea yet, a button to pick one.
 */
@Composable
internal fun TeaAndVesselSection(
    selectedTea: Tea?,
    teaTypeColorHex: String?,
    vessels: List<BrewingVessel>,
    selectedVessel: BrewingVessel?,
    onSelectTeaClicked: () -> Unit,
    onVesselSelected: (BrewingVessel) -> Unit,
    modifier: Modifier = Modifier,
    imageStorage: ImageStorage? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedTea != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeaHeader(
                    teaName = selectedTea.name,
                    vesselName = selectedVessel?.name,
                    teaColor = teaTypeColorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
                        ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectTeaClicked) {
                    Text(stringResource(Res.string.change))
                }
            }
        } else {
            Button(
                onClick = onSelectTeaClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.select_tea))
            }
        }
        VesselChipRow(
            vessels = vessels,
            selectedVessel = selectedVessel,
            onVesselSelected = onVesselSelected,
            imageStorage = imageStorage,
        )
    }
}

/**
 * Placeholder shown while the brewing parameters are gated behind tea + vessel selection.
 */
@Composable
internal fun SelectTeaAndVesselPrompt(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = FeatherIcons.Coffee,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(
            text = stringResource(Res.string.log_tea_choose_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.log_tea_choose_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/** The parameters' header, saying underneath where the fields were filled from when it was a default. */
@Composable
internal fun BrewingParametersHeader(
    prefillSource: PrefillSource,
    teaName: String,
    modifier: Modifier = Modifier,
) {
    val prefill = when (prefillSource) {
        is PrefillSource.TeaDefault -> stringResource(Res.string.prefill_tea_default, teaName)
        is PrefillSource.TypeDefault -> stringResource(Res.string.prefill_type_default, prefillSource.typeName)
        // A past brew shows under Start from; nothing to say without a pre-fill
        is PrefillSource.PastBrew, PrefillSource.None -> null
    }

    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionDivider()
        SectionHeader(stringResource(Res.string.header_brewing_parameters))
        prefill?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun BrewingTimeSection(
    brewingTime: Duration?,
    errorMessage: String?,
    onBrewingTimeChanged: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brewingTimePresets = remember {
        listOf(
            Preset("0:30", 30.seconds),
            Preset("0:45", 45.seconds),
            Preset("1:00", 1.minutes),
            Preset("1:30", 1.minutes + 30.seconds),
            Preset("2:00", 2.minutes),
            Preset("3:00", 3.minutes),
            Preset("4:00", 4.minutes),
            Preset("5:00", 5.minutes),
        )
    }

    Column(modifier = modifier) {
        DurationPicker(
            duration = brewingTime,
            onDurationChange = { duration ->
                duration?.let { onBrewingTimeChanged(it) }
            },
            isError = errorMessage != null,
            errorMessage = errorMessage,
            modifier = Modifier.fillMaxWidth(),
        )
        PresetChips(
            presets = brewingTimePresets,
            currentValue = brewingTime,
            onSelect = onBrewingTimeChanged,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Collapsible section for the rinse, water type and location.
 */
@Composable
internal fun OptionalDetailsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedWaterType: WaterType,
    rinsed: Boolean,
    location: String,
    onRinsedChange: (Boolean) -> Unit,
    onWaterTypeSelected: (WaterType) -> Unit,
    onLocationChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        SectionDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                title = stringResource(Res.string.header_optional_details),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) Res.string.collapse_optional_details else Res.string.expand_optional_details,
                ),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(0.dp))

                RinseSwitch(checked = rinsed, onCheckedChange = onRinsedChange)

                WaterTypeSelector(
                    selectedWaterType = selectedWaterType,
                    onWaterTypeSelected = onWaterTypeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChanged,
                    label = { Text(stringResource(Res.string.field_location_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
        }
    }
}

/**
 * Bottom action row - log the session as already brewed, or start the timer.
 */
@Composable
internal fun LogTeaActionButtons(
    onLogSessionClicked: () -> Unit,
    onBeginSessionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onLogSessionClicked,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                FeatherIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.log_session))
        }
        Button(
            onClick = onBeginSessionClicked,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                FeatherIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.begin_session))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaAndVesselSectionPreview() {
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
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TeaAndVesselSection(
                selectedTea = Tea(
                    id = "tea-1",
                    name = "Dragon Well",
                    teaTypeId = "green",
                    totalSessions = 12,
                    averageRating = 4.2f,
                    createdAt = PreviewInstant,
                    updatedAt = PreviewInstant,
                    syncStatus = SyncStatus.LOCAL_ONLY,
                ),
                teaTypeColorHex = "#4CAF50",
                vessels = vessels,
                selectedVessel = vessels.first(),
                onSelectTeaClicked = {},
                onVesselSelected = {},
            )
            TeaAndVesselSection(
                selectedTea = null,
                teaTypeColorHex = null,
                vessels = vessels,
                selectedVessel = null,
                onSelectTeaClicked = {},
                onVesselSelected = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingParameterSectionsPreview() {
    LeafLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BrewingParametersHeader(prefillSource = PrefillSource.TeaDefault, teaName = "Dragon Well")
            LeafQuantityField(
                isTeaBag = false,
                teaQuantityGrams = 5.0,
                onTeaBagModeChanged = {},
                onTeaQuantityChanged = {},
                presets = true,
                weightUnit = WeightUnit.GRAMS,
            )
            BrewingTimeSection(
                brewingTime = 2.minutes + 30.seconds,
                errorMessage = null,
                onBrewingTimeChanged = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionalDetailsSectionPreview() {
    LeafLogTheme {
        OptionalDetailsSection(
            expanded = true,
            onExpandedChange = {},
            selectedWaterType = WaterType.SPRING,
            rinsed = true,
            location = "Kitchen",
            onRinsedChange = {},
            onWaterTypeSelected = {},
            onLocationChanged = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogTeaActionButtonsPreview() {
    LeafLogTheme {
        LogTeaActionButtons(
            onLogSessionClicked = {},
            onBeginSessionClicked = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectTeaAndVesselPromptPreview() {
    LeafLogTheme {
        SelectTeaAndVesselPrompt()
    }
}

@Preview(showBackground = true)
@Composable
private fun SourceBrewSectionPreview() {
    val last = previewSession(id = "last", ratings = listOf(3f), status = SessionStatus.COMPLETED)
    val best = previewSession(
        id = "best",
        ratings = listOf(5f, 4.5f, 4f),
        status = SessionStatus.COMPLETED,
        notes = "Sweet from the second steep",
    ).let { session ->
        session.copy(steeps = session.steeps.mapIndexed { i, steep -> steep.copy(brewingTime = 10.seconds + 5.seconds * i) })
    }
    LeafLogTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SourceBrewSection(
                source = best,
                tags = emptyList(),
                lastBrew = last,
                bestBrew = best,
                onSourceSelected = {},
                onSourceClicked = {},
            )
            SourceBrewSection(
                source = null,
                tags = emptyList(),
                lastBrew = last,
                bestBrew = last,
                onSourceSelected = {},
                onSourceClicked = {},
            )
        }
    }
}
