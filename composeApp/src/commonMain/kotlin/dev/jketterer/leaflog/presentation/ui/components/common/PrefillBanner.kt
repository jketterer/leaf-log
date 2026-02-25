package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Info
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
/**
 * Banner that shows the source of pre-filled brewing parameters
 */
@Composable
fun PrefillBanner(
    source: PrefillSource,
    teaName: String,
    modifier: Modifier = Modifier,
    hasMultipleMethods: Boolean = false,
    onChooseDifferentMethod: (() -> Unit)? = null,
) {
    val bannerText = when (source) {
        is PrefillSource.SavedConfig -> {
            "Using your saved brewing method for $teaName"
        }

        is PrefillSource.SameTypeConfig -> {
            "Suggested parameters based on how you brew ${source.teaName}"
        }

        PrefillSource.None -> return // Don't show banner if no pre-fill
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FeatherIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (hasMultipleMethods && onChooseDifferentMethod != null) {
                TextButton(onClick = onChooseDifferentMethod) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
