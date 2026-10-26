package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.BrewingHabits
import dev.jketterer.leaflog.domain.models.BrewingVessel
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.VesselDistribution
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_favorite_vessel
import leaf_log.shared.generated.resources.recap_gongfu
import leaf_log.shared.generated.resources.recap_gongfu_sessions
import leaf_log.shared.generated.resources.recap_gongfu_steeps
import leaf_log.shared.generated.resources.recap_longest_session
import leaf_log.shared.generated.resources.recap_ratings_peak
import leaf_log.shared.generated.resources.recap_western
import leaf_log.shared.generated.resources.recap_western_sessions
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HowYouBrewedPage(howYouBrewed: RecapPage.HowYouBrewed, onTapSession: (sessionId: String) -> Unit) {
    val habits = howYouBrewed.habits
    RecapPageColumn {
        // Leads with the more common style, the other one a row below
        val gongfuLeads = habits.gongfuSessions >= habits.westernSessions
        val (lead, leadLabel) = if (gongfuLeads) {
            habits.gongfuSessions to Res.plurals.recap_gongfu_sessions
        } else {
            habits.westernSessions to Res.plurals.recap_western_sessions
        }
        Text(
            text = lead.toString(),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = pluralStringResource(leadLabel, lead),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        val (other, otherLabel) = if (gongfuLeads) {
            habits.westernSessions to Res.string.recap_western
        } else {
            habits.gongfuSessions to Res.string.recap_gongfu
        }
        if (other > 0) HabitRow(label = stringResource(otherLabel), value = other.toString())
        habits.gongfuAverageSteeps?.let {
            HabitRow(
                label = stringResource(Res.string.recap_gongfu_steeps),
                value = ((it * 10).roundToInt() / 10.0).toString(),
            )
        }
        howYouBrewed.peakSteep?.let {
            HabitRow(label = stringResource(Res.string.recap_ratings_peak), value = it.toString())
        }
        HabitRow(
            label = stringResource(Res.string.recap_longest_session),
            value = habits.longest.steeps.size.toString(),
            detail = howYouBrewed.longestTeaName,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onTapSession(habits.longest.session.id) },
        )
        howYouBrewed.favoriteVessel?.let {
            HabitRow(
                label = stringResource(Res.string.recap_favorite_vessel),
                value = it.sessionCount.toString(),
                detail = it.vessel.name,
            )
        }
    }
}

/** A [label], with a [detail] under it, beside its [value]. */
@Composable
private fun HabitRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    detail: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Preview
@Composable
private fun HowYouBrewedPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 5) {
            HowYouBrewedPage(
                howYouBrewed = RecapPage.HowYouBrewed(
                    habits = BrewingHabits(
                        gongfuSessions = 38,
                        westernSessions = 12,
                        gongfuAverageSteeps = 7.46,
                        longest = previewSession(
                            ratings = List(14) { null },
                            status = SessionStatus.COMPLETED,
                            brewingTime = 20.seconds,
                        ),
                    ),
                    longestTeaName = "Dan Cong Mi Lan Xiang",
                    peakSteep = 3,
                    favoriteVessel = VesselDistribution(
                        vessel = BrewingVessel(
                            id = "gaiwan",
                            name = "Gaiwan",
                            iconName = null,
                            isSystemDefault = true,
                            displayOrder = 0,
                            createdAt = PreviewInstant,
                            updatedAt = PreviewInstant,
                        ),
                        sessionCount = 31,
                        percentage = 62f,
                    ),
                ),
                onTapSession = {},
            )
        }
    }
}
