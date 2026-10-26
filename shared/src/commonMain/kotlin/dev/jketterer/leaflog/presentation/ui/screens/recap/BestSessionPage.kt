package dev.jketterer.leaflog.presentation.ui.screens.recap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.DateSkeleton
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.presentation.ui.components.common.formatRating
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.recap_best_session
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BestSessionPage(bestSession: RecapPage.BestSession, onTapSession: (sessionId: String) -> Unit) {
    val session = bestSession.session
    RecapPageColumn {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onTapSession(session.session.id) },
        ) {
            // A best session is rated by definition
            Text(
                text = formatRating(session.effectiveRating ?: 0f),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(Res.string.recap_best_session, bestSession.teaName),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = TimeFormatter.formatDate(
                    session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    DateSkeleton.WEEKDAY_MONTH_DAY,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            session.session.notes?.trim()?.lineSequence()?.firstOrNull()?.takeIf { it.isNotEmpty() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BestSessionPagePreview() {
    LeafLogTheme {
        RecapPageSurface(index = 6) {
            BestSessionPage(
                bestSession = RecapPage.BestSession(
                    session = previewSession(
                        ratings = listOf(5f, 5f, 4.5f, 4.5f),
                        status = SessionStatus.COMPLETED,
                        notes = "Honey and orchid all the way through, never went bitter\nRinsed once",
                    ),
                    teaName = "Dan Cong Mi Lan Xiang",
                ),
                onTapSession = {},
            )
        }
    }
}
