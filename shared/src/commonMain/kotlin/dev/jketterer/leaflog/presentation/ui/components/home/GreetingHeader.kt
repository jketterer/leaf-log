package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.currentInstant
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Greeting header with time-based message and current date subtitle.
 */
@Composable
fun GreetingHeader(
    modifier: Modifier = Modifier,
    greeting: String,
    userName: String? = null,
) {
    val today = currentInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dateSubtitle = TimeFormatter.formatDate(today, DateSkeleton.WEEKDAY_MONTH_DAY)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = if (userName != null) "$greeting, $userName!" else "$greeting!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = dateSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingHeaderPreview() {
    LeafLogTheme {
        Column {
            GreetingHeader(
                greeting = "Good morning",
                userName = null,
            )
            GreetingHeader(
                greeting = "Good afternoon",
                userName = "Tea Lover",
            )
            GreetingHeader(
                greeting = "Good evening",
                userName = null,
            )
        }
    }
}
