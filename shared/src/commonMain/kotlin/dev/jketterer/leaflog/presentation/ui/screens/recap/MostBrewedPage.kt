package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.presentation.ui.components.analytics.getChartColor
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.isDarkTheme
import kotlin.math.roundToInt
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_share
import leaf_log.shared.generated.resources.analytics_tea_types_summary
import leaf_log.shared.generated.resources.recap_most_brewed
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MostBrewedPage(mostBrewed: RecapPage.MostBrewed, onTapTea: (teaId: String) -> Unit) {
    RecapPageColumn {
        val top = mostBrewed.teas.first()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onTapTea(top.tea.id) },
        ) {
            Text(
                text = top.sessionCount.toString(),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = pluralStringResource(Res.plurals.recap_most_brewed, top.sessionCount, top.tea.name),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        mostBrewed.teas.drop(1).forEach { topTea ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onTapTea(topTea.tea.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = topTea.tea.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = topTea.sessionCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        // On surface, so tea type colors read as they do in Analytics
        Surface(shape = MaterialTheme.shapes.large) {
            TeaTypeSplit(mostBrewed.teaTypes, modifier = Modifier.padding(16.dp))
        }
    }
}

/** One bar shared out by tea type, with a legend of each type's share. */
@Composable
private fun TeaTypeSplit(teaTypes: List<TeaTypeDistribution>, modifier: Modifier = Modifier) {
    val dark = isDarkTheme()
    val colors = teaTypes.mapIndexed { index, it -> getChartColor(it.teaType.colorHex, index, dark) }
    val shares = teaTypes.map {
        stringResource(Res.string.analytics_share, it.teaType.name, it.percentage.roundToInt())
    }
    val summary = stringResource(Res.string.analytics_tea_types_summary, shares.joinToString())
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = summary }) {
        // Segments sit 1dp apart on an outline, so each reads as stroked on any surface
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .padding(1.dp),
        ) {
            teaTypes.forEachIndexed { index, it ->
                Box(
                    modifier = Modifier
                        .weight(it.sessionCount.toFloat())
                        .fillMaxSize()
                        .background(colors[index]),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            shares.forEachIndexed { index, share ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[index])
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = share, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview
@Composable
private fun MostBrewedPagePreview() {
    fun tea(id: String, name: String) = Tea(
        id = id,
        name = name,
        teaTypeId = id,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
        syncStatus = SyncStatus.LOCAL_ONLY,
    )
    LeafLogTheme {
        RecapPageSurface(index = 2) {
            MostBrewedPage(
                mostBrewed = RecapPage.MostBrewed(
                    teas = listOf(
                        TopTea(tea("1", "Long Jing Shi Feng 2026 First Flush"), 14),
                        TopTea(tea("2", "Tie Guan Yin"), 9),
                        TopTea(tea("3", "Bai Hao Yin Zhen"), 6),
                    ),
                    teaTypes = listOf(
                        TeaTypeDistribution(TeaType(id = "1", name = "Green", colorHex = "#4CAF50"), 20, 50f),
                        TeaTypeDistribution(TeaType(id = "2", name = "Oolong", colorHex = "#FF9800"), 12, 30f),
                        TeaTypeDistribution(TeaType(id = "3", name = "White", colorHex = "#FFFFFF"), 6, 15f),
                        TeaTypeDistribution(TeaType(id = "4", name = "Pu-erh", colorHex = "#3E2723"), 2, 5f),
                    ),
                ),
                onTapTea = {},
            )
        }
    }
}
