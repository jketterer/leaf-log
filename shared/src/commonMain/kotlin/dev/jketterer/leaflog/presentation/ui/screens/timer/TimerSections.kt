package dev.jketterer.leaflog.presentation.ui.screens.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.TimerStatus
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.timer.QuickAdjustButtons
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.already_brewed
import leaf_log.shared.generated.resources.edit_parameters
import leaf_log.shared.generated.resources.finish_early
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.just_a_timer
import leaf_log.shared.generated.resources.log_it
import leaf_log.shared.generated.resources.pause
import leaf_log.shared.generated.resources.quick_timer
import leaf_log.shared.generated.resources.reset
import leaf_log.shared.generated.resources.resume
import leaf_log.shared.generated.resources.review_next_in
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.what_are_you_brewing
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val RingMinSize = 160.dp
private val RingMaxSize = 280.dp
private val RingControlsGap = 16.dp
private val DialMargin = 24.dp

/**
 * The header on top, then the ring with its controls under it, centered in the height left over.
 * The ring takes that height, between [RingMinSize] and [RingMaxSize] (and 3/4 of the width), so
 * it is sized once by the screen and nothing below it changes. Where even the smallest ring
 * doesn't fit, the page grows past [viewportHeight] to scroll instead of overlapping the header.
 */
@Composable
internal fun TimerPageLayout(
    viewportHeight: Dp,
    header: @Composable () -> Unit,
    ring: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(header, ring, controls), modifier = modifier) { (h, r, c), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val headerPlaceable = h.single().measure(loose)
        val controlsPlaceable = c.single().measure(loose)

        val gap = RingControlsGap.roundToPx()
        val margin = DialMargin.roundToPx()
        val viewport = viewportHeight.roundToPx()
        val maxRing = min(RingMaxSize.roundToPx(), width * 3 / 4)
        val minRing = min(RingMinSize.roundToPx(), maxRing)
        val leftover = viewport - headerPlaceable.height - 2 * margin - gap - controlsPlaceable.height
        val ringSize = leftover.coerceIn(minRing, maxRing)
        val ringPlaceable = r.single().measure(Constraints.fixed(ringSize, ringSize))

        val dialHeight = ringSize + gap + controlsPlaceable.height
        val height = max(viewport, headerPlaceable.height + 2 * margin + dialHeight)
        val dialTop = headerPlaceable.height + (height - headerPlaceable.height - dialHeight) / 2
        layout(width, height) {
            headerPlaceable.place(0, 0)
            ringPlaceable.place((width - ringSize) / 2, dialTop)
            controlsPlaceable.place((width - controlsPlaceable.width) / 2, dialTop + ringSize + gap)
        }
    }
}

/** The steep under the tea's header, then what it brews with, tapped to change them. */
@Composable
internal fun SteepTimerDetailsSection(
    steepNumber: Int,
    temperature: String,
    water: String,
    leaf: String?,
    onEditParameters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.review_steep_n, steepNumber),
            style = MaterialTheme.typography.displaySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BrewingParamChip(FeatherIcons.Thermometer, temperature)
                BrewingParamChip(FeatherIcons.Droplet, water)
                leaf?.let { BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), it) }
            }
            IconButton(onClick = onEditParameters) {
                Icon(
                    imageVector = FeatherIcons.Edit2,
                    contentDescription = stringResource(Res.string.edit_parameters),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A Quick Timer has no tea yet, so it asks for one. */
@Composable
internal fun QuickTimerHeaderSection(
    onPickTea: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.quick_timer),
            style = MaterialTheme.typography.displaySmall,
        )
        AssistChip(
            onClick = onPickTea,
            label = { Text(stringResource(Res.string.what_are_you_brewing)) },
            leadingIcon = {
                Icon(
                    FeatherIcons.Coffee,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    }
}

/**
 * Under the ring: the adjust buttons while it counts, and once a Quick Timer is done, the next
 * steep by feel in their place. Both hold the same slot, so the ring above never moves.
 */
@Composable
internal fun TimerControlsSection(
    planned: Duration,
    isComplete: Boolean,
    // Null unless a Quick Timer is done
    nextByFeel: Duration?,
    onAdjust: (Duration) -> Unit,
    onNextByFeel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adjustAlpha by animateFloatAsState(if (isComplete) 0f else 1f, label = "adjust_alpha")
    val nextAlpha by animateFloatAsState(if (nextByFeel != null) 1f else 0f, label = "next_alpha")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        QuickAdjustButtons(
            planned = planned,
            onAdjust = onAdjust,
            enabled = !isComplete,
            modifier = Modifier.alpha(adjustAlpha),
        )
        if (nextByFeel != null) {
            FilledTonalButton(onClick = onNextByFeel, modifier = Modifier.alpha(nextAlpha)) {
                Text(stringResource(Res.string.review_next_in, formatBrewingTime(nextByFeel.inWholeSeconds.toInt())))
            }
        }
    }
}

/**
 * What the timer's state lets the user do, outlined beside filled. One row in every state, so the
 * ring above it never moves.
 */
@Composable
internal fun TimerBottomBar(
    status: TimerStatus,
    isQuickTimer: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    // Already brewed before Start, Finish early while running
    onCompleteNow: () -> Unit,
    onJustATimer: () -> Unit,
    onLogIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier) {
        Column {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AnimatedContent(
                targetState = status,
                modifier = Modifier.fillMaxWidth(),
                label = "timer_actions",
            ) { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (status) {
                        TimerStatus.NOT_STARTED -> {
                            // A Quick Timer has no steep to record
                            if (!isQuickTimer) {
                                OutlinedButton(onClick = onCompleteNow, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.already_brewed))
                                }
                            }
                            Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.start_brewing))
                            }
                        }

                        TimerStatus.RUNNING -> {
                            OutlinedButton(onClick = onCompleteNow, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.finish_early))
                            }
                            Button(onClick = onPause, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.pause))
                            }
                        }

                        TimerStatus.PAUSED -> {
                            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.reset))
                            }
                            Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.resume))
                            }
                        }

                        // Only a Quick Timer stays here once done: a session's steep moves on to its review
                        TimerStatus.COMPLETE -> {
                            OutlinedButton(onClick = onJustATimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.just_a_timer))
                            }
                            Button(onClick = onLogIt, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.log_it))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepTimerDetailsSectionPreview() {
    LeafLogTheme {
        SteepTimerDetailsSection(
            steepNumber = 3,
            temperature = "95°C",
            water = "120 mL",
            leaf = "7 g",
            onEditParameters = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTimerHeaderSectionPreview() {
    LeafLogTheme {
        QuickTimerHeaderSection(onPickTea = {}, modifier = Modifier.padding(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerControlsSectionDonePreview() {
    LeafLogTheme {
        TimerControlsSection(
            planned = 20.seconds,
            isComplete = true,
            nextByFeel = 25.seconds,
            onAdjust = {},
            onNextByFeel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerBottomBarNotStartedPreview() {
    LeafLogTheme {
        TimerBottomBar(
            status = TimerStatus.NOT_STARTED,
            isQuickTimer = false,
            onStart = {},
            onPause = {},
            onResume = {},
            onReset = {},
            onCompleteNow = {},
            onJustATimer = {},
            onLogIt = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerBottomBarPausedPreview() {
    LeafLogTheme {
        TimerBottomBar(
            status = TimerStatus.PAUSED,
            isQuickTimer = false,
            onStart = {},
            onPause = {},
            onResume = {},
            onReset = {},
            onCompleteNow = {},
            onJustATimer = {},
            onLogIt = {},
        )
    }
}
