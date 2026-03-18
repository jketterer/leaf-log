package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Droplet
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Thermometer
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.ic_tea_leaf
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SteepParameterCard(
    session: TeaSession,
    userPreferences: UserPreferences,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteepParameterCard(
        steepNumber = session.steepNumber,
        teaQuantity = session.teaQuantityGrams?.let { "${it}g" },
        waterQuantity = VolumeFormatter.format(
            session.waterQuantityMl,
            userPreferences.volumeUnit,
        ),
        temperature = TemperatureFormatter.format(
            session.temperatureCelsius,
            userPreferences.temperatureUnit,
        ),
        brewingTime = formatBrewingTime(session.brewingTime.inWholeSeconds.toInt()),
        onEditClick = onEditClick,
        modifier = modifier,
    )
}

@Composable
fun SteepParameterCard(
    steepNumber: Int = 1,
    teaQuantity: String? = null,
    waterQuantity: String? = null,
    temperature: String? = null,
    brewingTime: String? = null,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Steep $steepNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        FeatherIcons.Edit,
                        contentDescription = "Edit parameters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ParameterRow(
                    icon = vectorResource(Res.drawable.ic_tea_leaf),
                    label = "Tea quantity",
                    value = teaQuantity?.takeIf { it.isNotBlank() },
                )
                ParameterRow(
                    icon = FeatherIcons.Droplet,
                    label = "Water",
                    value = waterQuantity?.takeIf { it.isNotBlank() },
                )
                ParameterRow(
                    icon = FeatherIcons.Thermometer,
                    label = "Temperature",
                    value = temperature?.takeIf { it.isNotBlank() },
                )
                ParameterRow(
                    icon = FeatherIcons.Clock,
                    label = "Time",
                    value = brewingTime?.takeIf { it.isNotBlank() },
                )
            }
        }
    }
}

private const val PLACEHOLDER = "---"

@Preview(showBackground = true)
@Composable
private fun SteepParameterCardPreview() {
    LeafLogTheme {
        SteepParameterCard(
            steepNumber = 1,
            teaQuantity = "5g",
            waterQuantity = "200ml",
            temperature = "95\u00B0C",
            brewingTime = "3m 30s",
            onEditClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SteepParameterCardPartialPreview() {
    LeafLogTheme {
        SteepParameterCard(
            steepNumber = 3,
            teaQuantity = null,
            waterQuantity = "150ml",
            temperature = null,
            brewingTime = "45s",
            onEditClick = {},
        )
    }
}

@Composable
private fun ParameterRow(
    icon: ImageVector,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val hasValue = value != null
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value ?: PLACEHOLDER,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (hasValue) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
