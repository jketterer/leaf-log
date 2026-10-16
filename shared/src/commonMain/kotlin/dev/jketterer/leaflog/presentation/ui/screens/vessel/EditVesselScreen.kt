package dev.jketterer.leaflog.presentation.ui.screens.vessel

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
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Add/Edit Vessel Screen - create or edit a brewing vessel.
 */
@Composable
fun EditVesselScreen(
    vesselId: String?,
    onNavigateBack: () -> Unit,
    viewModel: EditVesselViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(vesselId) {
        viewModel.onIntent(EditVesselIntent.LoadVessel(vesselId))
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditVesselNavEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // System back has to reach the same guard as the toolbar arrow, or a swipe back silently
    // discards edits and orphans any photo already written to disk. Enabled only when there is
    // something to lose, so predictive back keeps its peek animation on a pristine form.
    //
    // Lives out here with the rest of the navigation wiring rather than in the content: it needs a
    // NavigationEventDispatcher from the nav host, and a @Preview has none, so calling it inside
    // the previewable composable makes every preview of this screen throw.
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = state.hasChanges,
        onBackCompleted = { viewModel.onIntent(EditVesselIntent.BackClicked) },
    )

    EditVesselContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        // From vesselId, not state.isEditMode: the state defaults to add mode and only flips once
        // LoadVessel has run, which is a composition too late to decide about the keyboard.
        autoFocusName = vesselId == null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVesselContent(
    state: EditVesselState,
    onIntent: (EditVesselIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    autoFocusName: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val nameFocusRequester = remember { FocusRequester() }
    val capacityFocusRequester = remember { FocusRequester() }

    // Keyboard up and ready when the user came here to type. Never in edit mode - that would throw
    // the keyboard over a form they opened to read.
    LaunchedEffect(autoFocusName) {
        if (autoFocusName) {
            nameFocusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (state.isEditMode) "Edit Vessel" else "Add Vessel") },
                    navigationIcon = {
                        IconButton(onClick = { onIntent(EditVesselIntent.BackClicked) }) {
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
                        onIntent(EditVesselIntent.SaveClicked)
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
            VesselIdentitySection(
                name = state.name,
                nameError = state.nameError,
                selectedIconName = state.selectedIconName,
                onNameChanged = { onIntent(EditVesselIntent.NameChanged(it)) },
                onIconSelected = { onIntent(EditVesselIntent.IconSelected(it)) },
                nameFocusRequester = nameFocusRequester,
                onNameImeNext = { capacityFocusRequester.requestFocus() },
            )

            VesselDetailsSection(
                capacity = state.capacity,
                capacityError = state.capacityError,
                volumeUnit = state.userPreferences.volumeUnit,
                onCapacityChanged = { onIntent(EditVesselIntent.CapacityChanged(it)) },
                onToggleVolumeUnit = { onIntent(EditVesselIntent.ToggleVolumeUnit) },
                capacityFocusRequester = capacityFocusRequester,
            )

            VesselPhotoSection(
                imagePath = state.imagePath,
                onPhotoPicked = { onIntent(EditVesselIntent.PhotoSelected(it)) },
                onRemovePhoto = { onIntent(EditVesselIntent.RemovePhoto) },
            )
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(EditVesselIntent.CancelDiscard) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onIntent(EditVesselIntent.ConfirmDiscard)
                    },
                ) {
                    Text(
                        text = "Discard",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(EditVesselIntent.CancelDiscard) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/** Names what is actually missing, rather than "the form is invalid". */
private fun missingFieldsMessage(state: EditVesselState): String = when {
    state.name.isBlank() -> "Add a name before saving"
    else -> "Fix the highlighted fields before saving"
}

@Preview(showBackground = true)
@Composable
private fun EditVesselScreenAddModePreview() {
    LeafLogTheme {
        EditVesselContent(
            state = EditVesselState(isEditMode = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditVesselScreenEditModePreview() {
    LeafLogTheme {
        EditVesselContent(
            state = EditVesselState(
                isEditMode = true,
                name = "Yixing Gaiwan",
                selectedIconName = "gaiwan",
                capacity = "120",
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditVesselScreenErrorsPreview() {
    LeafLogTheme {
        EditVesselContent(
            state = EditVesselState(
                isEditMode = false,
                name = "",
                nameError = "Name is required",
                capacity = "abc",
                capacityError = "Must be a valid number",
            ),
            onIntent = {},
        )
    }
}
