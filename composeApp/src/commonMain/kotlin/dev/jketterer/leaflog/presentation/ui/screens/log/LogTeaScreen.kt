package dev.jketterer.leaflog.presentation.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/**
 * Log Tea Screen - for creating new brewing sessions.
 */
@Composable
fun LogTeaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTimer: () -> Unit,
    viewModel: LogTeaViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LogTeaContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToTimer = onNavigateToTimer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTeaContent(
    state: LogTeaState,
    onIntent: (LogTeaIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTimer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Tea Session") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LogTeaIntent.BackClicked) }) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tea Selection
            Text(
                text = "Tea",
                style = MaterialTheme.typography.labelLarge
            )

            Button(
                onClick = { onIntent(LogTeaIntent.ShowTeaSearchDialog) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(state.selectedTea?.name ?: "Select Tea")
            }

            if (state.teaError != null) {
                Text(
                    text = state.teaError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Water Quantity
            OutlinedTextField(
                value = state.waterQuantityMl,
                onValueChange = { onIntent(LogTeaIntent.WaterQuantityChanged(it)) },
                label = { Text("Water Quantity (ml)") },
                isError = state.waterQuantityError != null,
                supportingText = state.waterQuantityError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Temperature
            OutlinedTextField(
                value = state.temperatureCelsius,
                onValueChange = { onIntent(LogTeaIntent.TemperatureChanged(it)) },
                label = { Text("Temperature (°C)") },
                isError = state.temperatureError != null,
                supportingText = state.temperatureError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Brewing Time
            // TODO: Add duration picker component

            // Vessel Selection
            // TODO: Add vessel dropdown

            // Water Type Selection
            // TODO: Add water type dropdown

            // Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onIntent(LogTeaIntent.SaveAsDraft) },
                    modifier = Modifier.weight(1f),
                    enabled = state.canSave
                ) {
                    Text("Save as Draft")
                }

                Button(
                    onClick = { onIntent(LogTeaIntent.SaveAsCompleted) },
                    modifier = Modifier.weight(1f),
                    enabled = state.canSave
                ) {
                    Text("Save & Complete")
                }
            }

            // Start Timer Button (Primary Action)
            Button(
                onClick = onNavigateToTimer,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave
            ) {
                Text("Start Timer & Brew")
            }

            // Error display
            state.error?.let { error ->
                Snackbar {
                    Text(error)
                }
            }
        }
    }

    // Tea Search Dialog
    if (state.showTeaSearchDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(LogTeaIntent.HideTeaSearchDialog) },
            title = { Text("Select Tea") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.teaSearchQuery,
                        onValueChange = { onIntent(LogTeaIntent.TeaSearchQueryChanged(it)) },
                        label = { Text("Search teas") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    state.availableTeas.forEach { tea ->
                        TextButton(
                            onClick = {
                                onIntent(LogTeaIntent.TeaSelected(tea))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tea.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(LogTeaIntent.QuickAddTeaClicked) }) {
                    Text("Add New Tea")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(LogTeaIntent.HideTeaSearchDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
private fun LogTeaScreenPreview() {
    LeafLogTheme {
        LogTeaContent(
            state = LogTeaState(
               selectedTea = Tea(
                   id = "",
                   name = "Test Tea",
                   teaTypeId = "",
                   createdAt = Instant.fromEpochMilliseconds(1),
                   updatedAt = Instant.fromEpochMilliseconds(1),
               ),
            ),
            onIntent = {},
            onNavigateToTimer = {},
            onNavigateBack = {},
        )
    }
}