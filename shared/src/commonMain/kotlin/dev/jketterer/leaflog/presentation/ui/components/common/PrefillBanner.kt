package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FeatherIcons
import compose.icons.feathericons.Info
import dev.jketterer.leaflog.domain.usecases.session.PrefillSource
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.prefill_tea_default
import leaf_log.shared.generated.resources.prefill_type_default
import org.jetbrains.compose.resources.stringResource

/**
 * Banner that shows the source of pre-filled brewing parameters
 */
@Composable
fun PrefillBanner(
    source: PrefillSource,
    teaName: String,
    modifier: Modifier = Modifier,
) {
    val bannerText = when (source) {
        is PrefillSource.TeaDefault -> stringResource(Res.string.prefill_tea_default, teaName)
        is PrefillSource.TypeDefault -> stringResource(Res.string.prefill_type_default, source.typeName)
        // A past brew shows as its own card; nothing to say without a pre-fill
        is PrefillSource.PastBrew, PrefillSource.None -> return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
    }
}

@Preview(showBackground = true)
@Composable
private fun PrefillBannerTeaDefaultPreview() {
    LeafLogTheme {
        PrefillBanner(
            source = PrefillSource.TeaDefault,
            teaName = "Dragon Well",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrefillBannerTypeDefaultPreview() {
    LeafLogTheme {
        PrefillBanner(
            source = PrefillSource.TypeDefault(typeName = "Green"),
            teaName = "Dragon Well",
        )
    }
}
