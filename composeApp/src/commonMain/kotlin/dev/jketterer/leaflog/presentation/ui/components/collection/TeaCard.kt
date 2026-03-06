package dev.jketterer.leaflog.presentation.ui.components.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Heart
import compose.icons.fontawesomeicons.solid.Heart
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.components.analytics.hexToColor
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.Instant
import org.koin.compose.koinInject

/**
 * Card component displaying tea summary information.
 *
 * When [teaTypeColorHex] is provided, a colored left border is shown.
 * When [trailingContent] is provided, it replaces the favorite button.
 */
@Composable
fun TeaCard(
    tea: Tea,
    teaTypeName: String,
    onTeaClick: () -> Unit,
    modifier: Modifier = Modifier,
    teaTypeColorHex: String? = null,
    onFavoriteClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    imageStorage: ImageStorage = koinInject(),
) {
    val accentColor = teaTypeColorHex?.hexToColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTeaClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor),
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (accentColor != null) 12.dp else 16.dp,
                        end = 16.dp,
                        top = if (accentColor != null) 10.dp else 16.dp,
                        bottom = if (accentColor != null) 10.dp else 16.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tea photo
                if (tea.photos.isNotEmpty()) {
                    AsyncImage(
                        model = imageStorage.resolveImagePath(tea.photos.first()),
                        contentDescription = tea.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Tea info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tea.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val detailInfo = buildList {
                        add(teaTypeName)
                        tea.origin?.let { add(it) }
                    }
                    Text(
                        text = detailInfo.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val brewStats = buildList {
                        if (tea.totalSessions > 0) add("${tea.totalSessions} brews")
                        tea.averageRating?.let { add("★ ${it.formatOneDecimal()}") }
                    }
                    if (brewStats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = brewStats.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Trailing content
                when {
                    trailingContent != null -> trailingContent()
                    onFavoriteClick != null -> {
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (tea.isFavorite) {
                                    FontAwesomeIcons.Solid.Heart
                                } else {
                                    FontAwesomeIcons.Regular.Heart
                                },
                                contentDescription = if (tea.isFavorite) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                },
                                tint = if (tea.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Float.formatOneDecimal(): String {
    val tenths = (this * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Preview
@Composable
private fun TeaCardPreview() {
    LeafLogTheme {
        TeaCard(
            tea = Tea(
                id = "1",
                name = "Test Tea",
                teaTypeId = "1",
                origin = "China",
                totalSessions = 12,
                averageRating = 4.2f,
                createdAt = Instant.fromEpochMilliseconds(1),
                updatedAt = Instant.fromEpochMilliseconds(1),
            ),
            teaTypeName = "Black Tea",
            onTeaClick = {},
            onFavoriteClick = {},
        )
    }
}

@Preview
@Composable
private fun TeaCardWithBorderPreview() {
    LeafLogTheme {
        TeaCard(
            tea = Tea(
                id = "1",
                name = "Dragon Well",
                teaTypeId = "green",
                origin = "India",
                totalSessions = 8,
                averageRating = 4.5f,
                createdAt = Instant.fromEpochMilliseconds(1),
                updatedAt = Instant.fromEpochMilliseconds(1),
            ),
            teaTypeName = "Green",
            teaTypeColorHex = "#4CAF50",
            onTeaClick = {},
            onFavoriteClick = {},
        )
    }
}
