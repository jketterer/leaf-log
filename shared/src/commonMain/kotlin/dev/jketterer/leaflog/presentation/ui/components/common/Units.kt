package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.jketterer.leaflog.domain.models.TemperatureFormatter
import dev.jketterer.leaflog.domain.models.TemperatureUnit
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.VolumeFormatter
import dev.jketterer.leaflog.domain.models.VolumeUnit
import dev.jketterer.leaflog.domain.models.WeightFormatter
import dev.jketterer.leaflog.domain.models.WeightUnit

/** The units Settings shows values in. */
data class Units(
    val temperature: TemperatureUnit,
    val volume: VolumeUnit,
    val weight: WeightUnit,
) {
    constructor(preferences: UserPreferences) : this(
        temperature = preferences.temperatureUnit,
        volume = preferences.volumeUnit,
        weight = preferences.weightUnit,
    )
}

/** Settings' units, provided at the root. Previews get the defaults. */
val LocalUnits = staticCompositionLocalOf { Units(UserPreferences()) }

/** [celsius] in Settings' temperature unit, or [unit] when a screen has switched it. */
@Composable
fun formatTemperature(celsius: Double, unit: TemperatureUnit = LocalUnits.current.temperature): String =
    TemperatureFormatter.format(celsius, unit)

/** [milliliters] in Settings' volume unit, or [unit] when a screen has switched it. */
@Composable
fun formatVolume(milliliters: Double, unit: VolumeUnit = LocalUnits.current.volume): String =
    VolumeFormatter.format(milliliters, unit)

/** [grams] in Settings' weight unit, or [unit] when a screen has switched it. */
@Composable
fun formatWeight(grams: Double, unit: WeightUnit = LocalUnits.current.weight): String =
    WeightFormatter.format(grams, unit)

@Composable
fun formatWeight(grams: Float, unit: WeightUnit = LocalUnits.current.weight): String =
    formatWeight(grams.toDouble(), unit)
