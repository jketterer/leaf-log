package dev.jketterer.leaflog.presentation.ui.screens.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * Add/Edit Tea Screen - create or edit a tea in the collection.
 */
@Composable
fun EditTeaScreen(
    teaId: String?,
    onNavigateBack: () -> Unit,
    viewModel: EditTeaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teaId) {
        viewModel.onIntent(EditTeaIntent.LoadTea(teaId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditTeaNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    EditTeaContent(
        state = state,
        onIntent = viewModel::onIntent,
        // From teaId, not state.isEditMode: the state defaults to add mode and only flips once
        // LoadTea has run, which is a composition too late to decide about the keyboard.
        autoFocusName = teaId == null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeaContent(
    state: EditTeaState,
    onIntent: (EditTeaIntent) -> Unit,
    autoFocusName: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val nameFocusRequester = remember { FocusRequester() }
    val originFocusRequester = remember { FocusRequester() }
    val producerFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    // System back has to reach the same guard as the toolbar arrow, or a swipe back silently
    // discards edits and orphans any photos already written to disk. Enabled only when there is
    // something to lose, so predictive back keeps its peek animation on a pristine form.
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = state.hasChanges,
        onBackCompleted = { onIntent(EditTeaIntent.BackClicked) },
    )

    // Keyboard up and ready when the user came here to type. Never in edit mode - that would throw
    // the keyboard over a form they opened to read.
    LaunchedEffect(autoFocusName) {
        if (autoFocusName) {
            nameFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onIntent(EditTeaIntent.ErrorShown)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (state.isEditMode) "Edit Tea" else "Add Tea") },
                    navigationIcon = {
                        IconButton(onClick = { onIntent(EditTeaIntent.BackClicked) }) {
                            Icon(
                                imageVector = FeatherIcons.ArrowLeft,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
                if (state.isLoading || state.isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            // Hidden while typing: the nav host applies imePadding to the whole screen, so a
            // bottom-anchored FAB would otherwise sit directly on top of the field in use.
            // The inset height rather than isImeVisible, which is Android-only.
            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            AnimatedVisibility(
                visible = !imeVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Save") },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.Save,
                            contentDescription = null,
                        )
                    },
                    // Always enabled. A dead button explains nothing; tapping an invalid form
                    // populates the field errors and says what is missing.
                    onClick = {
                        onIntent(EditTeaIntent.SaveClicked)
                        if (!state.isValid) {
                            if (state.name.isBlank()) {
                                nameFocusRequester.requestFocus()
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(missingFieldsMessage(state))
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                // Bottom room so the last field clears the FAB.
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TeaIdentitySection(
                name = state.name,
                nameError = state.nameError,
                teaTypes = state.availableTeaTypes,
                selectedTeaTypeId = state.selectedTeaTypeId,
                teaTypeError = state.teaTypeError,
                onNameChanged = { onIntent(EditTeaIntent.NameChanged(it)) },
                onTeaTypeSelected = { onIntent(EditTeaIntent.TeaTypeSelected(it)) },
                nameFocusRequester = nameFocusRequester,
                onNameImeNext = { originFocusRequester.requestFocus() },
            )

            TeaDetailsSection(
                origin = state.origin,
                producer = state.producer,
                availableProducers = state.availableProducers,
                purchaseDate = state.purchaseDate,
                description = state.description,
                temperatureDisplay = state.temperatureDisplayValue,
                temperatureUnit = state.userPreferences.temperatureUnit,
                temperatureError = state.temperatureError,
                onOriginChanged = { onIntent(EditTeaIntent.OriginChanged(it)) },
                onProducerChanged = { onIntent(EditTeaIntent.ProducerChanged(it)) },
                onPurchaseDateChanged = { onIntent(EditTeaIntent.PurchaseDateChanged(it)) },
                onDescriptionChanged = { onIntent(EditTeaIntent.DescriptionChanged(it)) },
                onTemperatureChanged = { onIntent(EditTeaIntent.TemperatureChanged(it)) },
                onToggleTemperatureUnit = { onIntent(EditTeaIntent.ToggleTemperatureUnit) },
                originFocusRequester = originFocusRequester,
                producerFocusRequester = producerFocusRequester,
                descriptionFocusRequester = descriptionFocusRequester,
            )

            TeaPhotosSection(
                photos = state.photos,
                onAddPhoto = { onIntent(EditTeaIntent.PhotoSelected(it)) },
                onRemovePhoto = { onIntent(EditTeaIntent.PhotoRemoved(it)) },
            )
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(EditTeaIntent.CancelDiscard) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onIntent(EditTeaIntent.ConfirmDiscard)
                    },
                ) {
                    Text(
                        text = "Discard",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(EditTeaIntent.CancelDiscard) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/** Names what is actually missing, rather than "the form is invalid". */
private fun missingFieldsMessage(state: EditTeaState): String {
    val missing = buildList {
        if (state.name.isBlank()) add("a name")
        if (state.selectedTeaTypeId == null) add("a tea type")
    }
    return when {
        missing.isEmpty() -> "Fix the highlighted fields before saving"
        else -> "Add ${missing.joinToString(" and ")} before saving"
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaScreenAddModePreview() {
    LeafLogTheme {
        EditTeaContent(
            state = EditTeaState(
                isEditMode = false,
                name = "",
                availableTeaTypes = previewScreenTeaTypes,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaScreenEditModePreview() {
    LeafLogTheme {
        EditTeaContent(
            state = EditTeaState(
                isEditMode = true,
                name = "Dragon Well",
                selectedTeaTypeId = "green",
                origin = "Hangzhou, China",
                producer = "West Lake Tea Company",
                defaultTemperatureCelsius = "80",
                description = "Premium Dragon Well green tea",
                availableTeaTypes = previewScreenTeaTypes,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTeaScreenErrorsPreview() {
    LeafLogTheme {
        EditTeaContent(
            state = EditTeaState(
                isEditMode = false,
                name = "",
                nameError = "Name is required",
                teaTypeError = "Tea type is required",
                availableTeaTypes = previewScreenTeaTypes,
            ),
            onIntent = {},
        )
    }
}

private val previewScreenTeaTypes = listOf(
    TeaType(
        id = "green",
        name = "Green",
        defaultTemperatureCelsius = 75.0,
        colorHex = "#4CAF50",
        isSystemDefault = true,
        displayOrder = 0,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
    TeaType(
        id = "black",
        name = "Black",
        defaultTemperatureCelsius = 95.0,
        colorHex = "#795548",
        isSystemDefault = true,
        displayOrder = 1,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    ),
)
