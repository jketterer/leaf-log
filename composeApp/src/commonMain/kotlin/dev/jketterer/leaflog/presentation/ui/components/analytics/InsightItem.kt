package dev.jketterer.leaflog.presentation.ui.components.analytics

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Award
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Heart
import compose.icons.feathericons.PieChart
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Star
import compose.icons.feathericons.Thermometer
import compose.icons.feathericons.Zap
import dev.jketterer.leaflog.domain.models.InsightType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
fun InsightItem(
    type: InsightType,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val InsightType.icon: ImageVector
    get() = when (this) {
        InsightType.MOST_BREWED -> FeatherIcons.Coffee
        InsightType.AVERAGE_RATING -> FeatherIcons.Star
        InsightType.CONSISTENCY -> FeatherIcons.Zap
        InsightType.FAVORITE_TYPE -> FeatherIcons.PieChart
        InsightType.RE_STEEP -> FeatherIcons.RefreshCw
        InsightType.TOP_RATED -> FeatherIcons.Award
        InsightType.PREFERRED_VESSEL -> FeatherIcons.Heart
    }

@Preview(showBackground = true)
@Composable
private fun InsightItemPreview() {
    LeafLogTheme {
        InsightItem(
            type = InsightType.MOST_BREWED,
            text = "Dragon Well is your most brewed tea with 12 sessions",
        )
    }
}
