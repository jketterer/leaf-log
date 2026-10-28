package dev.jketterer.leaflog.presentation.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.RefreshCw
import dev.jketterer.leaflog.domain.models.Session
import dev.jketterer.leaflog.domain.models.TimeFormatter
import dev.jketterer.leaflog.domain.models.formatDate
import dev.jketterer.leaflog.presentation.ui.components.common.PhotoGrid
import dev.jketterer.leaflog.presentation.ui.components.common.RatingDisplay
import dev.jketterer.leaflog.presentation.ui.components.common.currentDate
import dev.jketterer.leaflog.presentation.ui.components.common.formatWeight
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.components.session.IconValue
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.date_at_time
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.review_tea_bag
import leaf_log.shared.generated.resources.rinsed
import leaf_log.shared.generated.resources.tea_not_found
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Instant

/** The tea, when, how it rated, how it was set up, the session's note and its photos. */
@Composable
internal fun SessionHeaderSection(
    teaName: String?,
    onTeaClick: (() -> Unit)?,
    startedAt: Instant,
    rating: Float?,
    session: Session,
    vesselName: String?,
    methodLabel: String?,
    photos: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = teaName ?: stringResource(Res.string.tea_not_found),
            style = MaterialTheme.typography.headlineSmall,
            color = if (onTeaClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (onTeaClick != null) Modifier.clickable(
                        role = Role.Button,
                        onClick = onTeaClick
                    )
                    else Modifier
                )
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )

        val dateTime = startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
        Text(
            text = stringResource(
                Res.string.date_at_time,
                TimeFormatter.formatDate(dateTime.date, currentDate()),
                TimeFormatter.formatClockTime(dateTime.hour, dateTime.minute),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RatingDisplay(rating)

        val style = MaterialTheme.typography.bodyMedium
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            vesselName?.let { IconValue(FeatherIcons.Coffee, it, style, iconSize = 16.dp) }
            IconValue(FeatherIcons.Droplet, session.waterType.displayName, style, iconSize = 16.dp)
            IconValue(
                icon = vectorResource(Res.drawable.ic_tea_leaf),
                text = session.teaQuantityGrams?.let { formatWeight(it) }
                    ?: stringResource(Res.string.review_tea_bag),
                style = style,
                iconSize = 16.dp,
            )
            if (session.rinsed) {
                IconValue(
                    FeatherIcons.RefreshCw,
                    stringResource(Res.string.rinsed),
                    style,
                    iconSize = 16.dp
                )
            }
            session.location?.let { IconValue(FeatherIcons.MapPin, it, style, iconSize = 16.dp) }
            methodLabel?.let { IconValue(FeatherIcons.Bookmark, it, style, iconSize = 16.dp) }
        }

        session.notes?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (photos.isNotEmpty()) {
            PhotoGrid(photos = photos, onAddPhoto = {}, onRemovePhoto = {}, enabled = false)
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun SessionHeaderSectionPreview() {
    val session =
        previewSession(ratings = listOf(4f, 5f), notes = "Opened up beautifully by the third steep")
    LeafLogTheme {
        SessionHeaderSection(
            teaName = "Da Hong Pao",
            onTeaClick = {},
            startedAt = session.startedAt,
            rating = session.effectiveRating,
            session = session.session.copy(rinsed = true, location = "Home"),
            vesselName = "Gaiwan",
            methodLabel = "Gongfu 7g",
            photos = emptyList(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
