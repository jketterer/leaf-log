package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.jketterer.leaflog.presentation.ui.navigation.LocalSharedTransitionScope
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import dev.jketterer.leaflog.presentation.ui.theme.teaTypeColor
import kotlinx.serialization.Serializable
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import leaf_log.shared.generated.resources.tea_not_found
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Which tea and vessel, over a brew screen's headline, beside a leaf on a tile in the tea type's color.
 * Given a [sharedKey], such as the session's id, it holds still as the screen changes to another
 * screen's header with the same key.
 */
@Composable
fun TeaHeader(
    teaName: String,
    vesselName: String?,
    teaColor: Color,
    modifier: Modifier = Modifier,
    sharedKey: Any? = null,
) {
    Row(
        modifier = modifier.sharedTeaHeader(sharedKey),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(teaColor.copy(alpha = 0.16f), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_tea_leaf),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = teaColor,
            )
        }
        // Weighted so a Row measuring its intrinsic height wraps the name as it will lay out
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = teaName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            vesselName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * What a brew flow screen's [TeaHeader] shows. A route carries it so the next screen draws its header
 * on its first frame, before it loads, where it holds still against the last screen's.
 */
@Serializable
data class TeaHeaderInfo(
    /** Keys the header shared across the session's screens. */
    val sessionId: String,
    /** Null for a tea since deleted. */
    val teaName: String?,
    val vesselName: String?,
    val colorHex: String?,
)

/** A brew flow screen's header, shared with the session's other screens. */
@Composable
fun TeaHeader(
    info: TeaHeaderInfo,
    modifier: Modifier = Modifier,
) {
    TeaHeader(
        teaName = info.teaName ?: stringResource(Res.string.tea_not_found),
        vesselName = info.vesselName,
        teaColor = info.colorHex?.let { teaTypeColor(it) }?.takeIf { it.isSpecified }
            ?: MaterialTheme.colorScheme.primary,
        modifier = modifier,
        sharedKey = info.sessionId,
    )
}

@Composable
private fun Modifier.sharedTeaHeader(key: Any?): Modifier {
    val scope = LocalSharedTransitionScope.current
    if (key == null || scope == null) return this
    val animatedContentScope = LocalNavAnimatedContentScope.current
    return with(scope) {
        sharedElement(rememberSharedContentState("tea-header/$key"), animatedContentScope)
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaHeaderPreview() {
    LeafLogTheme {
        TeaHeader(
            teaName = "Tie Guan Yin",
            vesselName = "Gaiwan",
            teaColor = Color(0xFFC27400),
            modifier = Modifier.padding(24.dp),
        )
    }
}
