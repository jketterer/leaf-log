package dev.jketterer.leaflog.presentation.ui.components.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.presentation.ui.components.common.DurationPicker
import dev.jketterer.leaflog.presentation.ui.components.common.Preset
import dev.jketterer.leaflog.presentation.ui.components.common.PresetChips
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.field_steep_increment
import leaf_log.shared.generated.resources.field_steep_increment_hint
import leaf_log.shared.generated.resources.header_steep_plan
import leaf_log.shared.generated.resources.plan_adjusted
import leaf_log.shared.generated.resources.plan_clear_adjusted
import leaf_log.shared.generated.resources.plan_steep_label
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Edits a method's increment and previews the steeps it plans. Overrides are learned from a
 * session, so they are marked and can be cleared but never edited.
 */
@Composable
fun BrewingPlanSection(
    plan: BrewingPlan,
    onIncrementChange: (Duration?) -> Unit,
    onClearOverrides: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = remember {
        listOf(5, 10, 15, 30, 60).map { Preset("+${formatBrewingTime(it)}", it.seconds) }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(Res.string.header_steep_plan))
        DurationPicker(
            duration = plan.increment,
            onDurationChange = onIncrementChange,
            label = stringResource(Res.string.field_steep_increment),
        )
        Text(
            text = stringResource(Res.string.field_steep_increment_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PresetChips(presets = presets, currentValue = plan.increment, onSelect = onIncrementChange)

        if (plan.increment != null || plan.overrides.isNotEmpty()) {
            val steeps = (1..plan.previewLength).map { it to plan.timeOf(it) }
            val longest = steeps.maxOf { it.second }
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steeps.forEach { (steep, time) ->
                    PlanRow(steep, time, fraction = (time / longest).toFloat(), adjusted = steep in plan.overrides)
                }
            }
            if (plan.overrides.isNotEmpty()) {
                TextButton(onClick = onClearOverrides) {
                    Text(stringResource(Res.string.plan_clear_adjusted))
                }
            }
        }
    }
}

/** One steep: its number, a bar sized against the longest steep shown, its time and an Adjusted tag. */
@Composable
private fun PlanRow(steep: Int, time: Duration, fraction: Float, adjusted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.plan_steep_label, steep),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(12.dp)
                    .background(
                        color = if (adjusted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
        Text(
            text = formatBrewingTime(time.inWholeSeconds.toInt()),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = if (adjusted) stringResource(Res.string.plan_adjusted) else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(60.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingPlanSectionPreview() {
    LeafLogTheme {
        BrewingPlanSection(
            plan = BrewingPlan(20.seconds, 5.seconds, mapOf(4 to 45.seconds, 8 to 50.seconds)),
            onIncrementChange = {},
            onClearOverrides = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrewingPlanSectionWesternPreview() {
    LeafLogTheme {
        BrewingPlanSection(
            plan = BrewingPlan(3.minutes),
            onIncrementChange = {},
            onClearOverrides = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
