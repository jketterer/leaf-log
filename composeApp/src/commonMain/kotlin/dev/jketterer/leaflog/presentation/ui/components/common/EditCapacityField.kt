package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jketterer.leaflog.domain.models.VolumeUnit

@Composable
fun EditCapacityField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String,
    enabled: Boolean,
    volumeUnit: VolumeUnit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Capacity") },
        isError = isError,
        supportingText = { Text(supportingText) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        suffix = { Text(volumeUnit.symbol) }
    )
}