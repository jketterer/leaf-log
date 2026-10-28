package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

/**
 * Which tea and vessel, as a small line over a brew screen's headline, after a dot in the tea
 * type's color. A long tea name wraps to two lines and pushes the vessel onto its own, so neither
 * is ever cut away whole.
 */
@Composable
fun TeaEyebrow(
    teaName: String,
    vesselName: String?,
    teaColor: Color,
    modifier: Modifier = Modifier,
) {
    val eyebrow = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Centred on the first line, however many the names take
        val lineHeight = with(LocalDensity.current) { eyebrow.lineHeight.toDp() }
        Box(modifier = Modifier.padding(top = (lineHeight - 8.dp) / 2).size(8.dp).background(teaColor, CircleShape))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = teaName.uppercase(),
                style = eyebrow,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            vesselName?.let {
                Text(
                    text = it.uppercase(),
                    style = eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeaEyebrowPreview() {
    LeafLogTheme {
        TeaEyebrow(
            teaName = "Tie Guan Yin",
            vesselName = "Gaiwan",
            teaColor = Color(0xFFC27400),
            modifier = Modifier.padding(24.dp),
        )
    }
}
