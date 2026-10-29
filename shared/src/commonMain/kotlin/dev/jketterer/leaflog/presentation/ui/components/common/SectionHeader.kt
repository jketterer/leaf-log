package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.header_recent_sessions
import leaf_log.shared.generated.resources.view_all
import org.jetbrains.compose.resources.stringResource

/**
 * The label above a group of content, uppercased here so [title] stays sentence case in
 * `strings.xml`. An [action] such as "View all" sits at the end.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        action?.invoke(this)
    }
}

/** The line above a [SectionHeader] that starts a new group, spaced so the header sits with what follows. */
@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier.padding(top = 8.dp, bottom = 16.dp))
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    LeafLogTheme {
        SectionHeader(title = stringResource(Res.string.header_recent_sessions))
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionDividerPreview() {
    LeafLogTheme {
        Column {
            SectionDivider()
            SectionHeader(title = stringResource(Res.string.header_recent_sessions))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderWithActionPreview() {
    LeafLogTheme {
        SectionHeader(title = stringResource(Res.string.header_recent_sessions)) {
            TextButton(onClick = {}) {
                Text(stringResource(Res.string.view_all))
            }
        }
    }
}
