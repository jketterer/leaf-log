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
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Greeting header with time-based message.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    userName: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (userName != null) "$greeting, $userName!" else "$greeting!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
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