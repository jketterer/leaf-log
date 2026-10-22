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
import androidx.compose.material3.TextButton
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
import leaf_log.shared.generated.resources.prefill_best_brew
import leaf_log.shared.generated.resources.prefill_change
import leaf_log.shared.generated.resources.prefill_same_type
import leaf_log.shared.generated.resources.prefill_saved_method
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
    hasMultipleMethods: Boolean = false,
    onChooseDifferentMethod: (() -> Unit)? = null,
) {
    val bannerText = when (source) {
        is PrefillSource.SavedConfig -> stringResource(Res.string.prefill_saved_method, teaName)
        is PrefillSource.BestRatedSession ->
            stringResource(Res.string.prefill_best_brew, teaName, formatRating(source.rating))
        is PrefillSource.SameTypeConfig -> stringResource(Res.string.prefill_same_type, source.teaName)
        is PrefillSource.TeaDefault -> stringResource(Res.string.prefill_tea_default, teaName)
        is PrefillSource.TypeDefault -> stringResource(Res.string.prefill_type_default, source.typeName)
        PrefillSource.None -> return // Don't show banner if no pre-fill
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
                        text = stringResource(Res.string.prefill_change),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrefillBannerBestRatedSessionPreview() {
    LeafLogTheme {
        PrefillBanner(
            source = PrefillSource.BestRatedSession(rating = 4.5f),
            teaName = "Dragon Well",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrefillBannerSavedConfigPreview() {
    LeafLogTheme {
        PrefillBanner(
            source = PrefillSource.SavedConfig,
            teaName = "Dragon Well",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrefillBannerSameTypePreview() {
    LeafLogTheme {
        PrefillBanner(
            source = PrefillSource.SameTypeConfig(teaName = "Longjing"),
            teaName = "Bi Luo Chun",
            hasMultipleMethods = true,
            onChooseDifferentMethod = {},
        )
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
