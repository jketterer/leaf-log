package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.BrewingPlan
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.presentation.ui.components.common.BrewingParamChip
import dev.jketterer.leaflog.presentation.ui.components.common.SectionHeader
import dev.jketterer.leaflog.presentation.ui.components.common.formatBrewingTime
import dev.jketterer.leaflog.presentation.ui.components.common.formatTemperature
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.LastTimeLine
import dev.jketterer.leaflog.presentation.ui.components.session.TeaHeader
import dev.jketterer.leaflog.presentation.ui.screens.home.BrewAgainCardData
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.resources.stringResource

/** Home's pairs to brew again, a card each; a tap opens the Brew again sheet on it. */
@Composable
fun BrewAgainRow(
    cards: List<BrewAgainCardData>,
    onCardClick: (BrewAgainCardData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // As tall as Home's headers with a View all button, so the gap to the cards matches theirs
        SectionHeader(
            title = stringResource(Res.string.brew_again),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .heightIn(min = 48.dp),
        )

        val horizontalPadding = 16.dp
        val cardSpacing = 8.dp

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Wide enough for a brew's chips and most tea names on one line, growing with the font, never past the screen
            val availableWidth = maxWidth - (horizontalPadding * 2)
            val cardWidth = (CARD_WIDTH * LocalDensity.current.fontScale).coerceAtMost(availableWidth)

            // At most 8 cards, so a plain Row, which lets every card take the tallest one's height
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
                cards.forEach { card ->
                    key(card.teaId, card.vesselId) {
                        BrewAgainCard(
                            card = card,
                            onClick = { onCardClick(card) },
                            modifier = Modifier
                                .width(cardWidth)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

private val CARD_WIDTH = 248.dp

@Composable
private fun BrewAgainCard(
    card: BrewAgainCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val teaColor = card.teaTypeColorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
        ?: MaterialTheme.colorScheme.primary
    val steep1 = card.brew.steeps.first()
    val plan = remember(card.brew) { BrewingPlan.learnedFrom(card.brew.steeps) }

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TeaHeader(
                teaName = card.teaName,
                vesselName = card.vesselName,
                teaColor = teaColor,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BrewingParamChip(FeatherIcons.Clock, formatBrewingTime(plan.base.inWholeSeconds.toInt()))
                BrewingParamChip(FeatherIcons.Thermometer, formatTemperature(steep1.temperatureCelsius))
                card.brew.session.teaQuantityGrams?.let {
                    BrewingParamChip(vectorResource(Res.drawable.ic_tea_leaf), formatWeight(it))
                }
            }
            // On the card's bottom edge when a taller card in the row stretches this one
            LastTimeLine(
                session = card.brew,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(Alignment.Bottom),
            )
        }
    }
}

private fun previewCard(
    teaName: String,
    vesselName: String,
    colorHex: String,
    ratings: List<Float?>,
    temperatureCelsius: Double,
    brewingTime: Duration,
    teaQuantityGrams: Float? = 5f,
) = BrewAgainCardData(
    teaId = teaName,
    vesselId = vesselName,
    teaName = teaName,
    teaTypeColorHex = colorHex,
    vesselName = vesselName,
    brew = previewSession(
        id = teaName,
        ratings = ratings,
        status = SessionStatus.COMPLETED,
        brewingTime = brewingTime,
        temperatureCelsius = temperatureCelsius,
        teaQuantityGrams = teaQuantityGrams,
    ),
)

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun BrewAgainRowPreview() {
    LeafLogTheme {
        BrewAgainRow(
            cards = listOf(
                previewCard("Tie Guan Yin", "Gaiwan", "#FF9800", List(10) { 4f }, 95.0, 1.minutes + 30.seconds),
                previewCard("English Breakfast", "Mug", "#795548", listOf(null), 100.0, 4.minutes),
                previewCard("Simao Spring Tips", "Gaiwan", "#E91E63", listOf(3f), 100.0, 4.minutes, teaQuantityGrams = null),
            ),
            onCardClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun BrewAgainRowLongTextPreview() {
    LeafLogTheme {
        BrewAgainRow(
            cards = listOf(
                previewCard(
                    "Wuyi Mountain Old Bush Shui Xian Spring Harvest",
                    "Hand-thrown Yixing Clay Teapot",
                    "#795548",
                    listOf(4.5f),
                    100.0,
                    15.seconds,
                ),
                previewCard("Sencha", "Kyusu", "#4CAF50", listOf(null), 80.0, 1.minutes),
            ),
            onCardClick = {},
        )
    }
}
