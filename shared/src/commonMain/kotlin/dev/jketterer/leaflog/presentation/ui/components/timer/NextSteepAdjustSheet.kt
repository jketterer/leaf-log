package dev.jketterer.leaflog.presentation.ui.components.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.presentation.ui.components.common.SheetPreviewContainer
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.start_steep_n
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The next steep's time and temperature, changed for that steep and those after it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextSteepAdjustSheet(
    nextSteepNumber: Int,
    duration: Duration?,
    temperatureCelsius: Double?,
    temperatureUnit: TemperatureUnit,
    onDurationChange: (Duration?) -> Unit,
    onTemperatureChange: (Double?) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        NextSteepAdjustContent(
            nextSteepNumber = nextSteepNumber,
            duration = duration,
            temperatureCelsius = temperatureCelsius,
            temperatureUnit = temperatureUnit,
            onDurationChange = onDurationChange,
            onTemperatureChange = onTemperatureChange,
            onStart = onStart,
        )
    }
}

@Composable
private fun NextSteepAdjustContent(
    nextSteepNumber: Int,
    duration: Duration?,
    temperatureCelsius: Double?,
    temperatureUnit: TemperatureUnit,
    onDurationChange: (Duration?) -> Unit,
    onTemperatureChange: (Double?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isValid = duration != null && duration > Duration.ZERO &&
            temperatureCelsius != null && temperatureCelsius > 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        NextSteepParameterDialogContent(
            nextSteepNumber = nextSteepNumber,
            duration = duration,
            temperatureCelsius = temperatureCelsius,
            temperatureUnit = temperatureUnit,
            onDurationChange = onDurationChange,
            onTemperatureChange = onTemperatureChange,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStart,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.start_steep_n, nextSteepNumber))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NextSteepAdjustContentPreview() {
    LeafLogTheme {
        SheetPreviewContainer(height = 700.dp) {
            NextSteepAdjustContent(
                nextSteepNumber = 3,
                duration = 25.seconds,
                temperatureCelsius = 95.0,
                temperatureUnit = TemperatureUnit.CELSIUS,
                onDurationChange = {},
                onTemperatureChange = {},
                onStart = {},
            )
        }
    }
}
