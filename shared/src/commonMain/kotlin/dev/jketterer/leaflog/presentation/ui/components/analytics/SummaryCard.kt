package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowDown
import compose.icons.feathericons.ArrowUp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_change_down
import leaf_log.shared.generated.resources.analytics_change_up
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SummaryCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    percentageChange: Float? = null,
    /** What [percentageChange] is measured from, such as "last week", for a screen reader. */
    previousPeriod: String = "",
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    supportingText: String? = null,
    /** A control in the top-end corner, such as a unit switch. It adds no height. */
    action: (@Composable () -> Unit)? = null,
) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val cardModifier = modifier.fillMaxHeight()
    val content: @Composable () -> Unit = {
        Box {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                val baseStyle = MaterialTheme.typography.headlineSmall
                var scaleFactor by remember { mutableFloatStateOf(1f) }
                Text(
                    text = value,
                    style = baseStyle.copy(fontSize = baseStyle.fontSize * scaleFactor),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow) {
                            scaleFactor *= 0.9f
                        }
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (supportingText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
                if (percentageChange != null && percentageChange.absoluteValue > 0.5f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isPositive = percentageChange > 0
                    val percent = percentageChange.absoluteValue.roundToInt()
                    // Neutral both ways: brewing less isn't a failing
                    val color = MaterialTheme.colorScheme.onSurfaceVariant
                    val description = stringResource(
                        if (isPositive) Res.string.analytics_change_up else Res.string.analytics_change_down,
                        percent,
                        previousPeriod,
                    )
                    Row(
                        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isPositive) FeatherIcons.ArrowUp else FeatherIcons.ArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = color,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                    }
                }
            }
            if (action != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) { action() }
            }
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = cardColors,
            elevation = cardElevation,
            content = { content() },
        )
    } else {
        Card(
            modifier = cardModifier,
            colors = cardColors,
            elevation = cardElevation,
            content = { content() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardPreview() {
    LeafLogTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SummaryCard(
                value = "42",
                label = "Sessions",
                percentageChange = 15f,
                previousPeriod = "last week",
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SummaryCard(
                value = "3.2L",
                label = "Water",
                percentageChange = -8f,
                previousPeriod = "last week",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
