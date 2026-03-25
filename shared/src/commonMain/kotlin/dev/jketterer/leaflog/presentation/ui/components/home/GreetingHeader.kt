package dev.jketterer.leaflog.presentation.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Greeting header with time-based message and current date subtitle.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    userName: String? = null,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val dayName = today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val dateSubtitle = "$dayName, $monthName ${today.day}"

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
