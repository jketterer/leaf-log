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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Save
import dev.jketterer.leaflog.domain.models.TeaType
import dev.jketterer.leaflog.presentation.ui.components.common.DiscardChangesGuardDialog
import dev.jketterer.leaflog.presentation.ui.components.common.PreviewInstant
import dev.jketterer.leaflog.presentation.ui.messages.UiMessageEffect
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.navigate_up
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Add/Edit Tea Screen - create or edit a tea in the collection.
 */
@Composable
fun EditTeaScreen(
    route: NavRoute.EditTeaRoute,
    onNavigateBack: () -> Unit,
    viewModel: EditTeaViewModel = koinViewModel { parametersOf(route) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    UiMessageEffect(viewModel.messages, snackbarHostState)

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is EditTeaNavEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    // Discarding also deletes any photos already written to disk, or they'd be orphaned.
    DiscardChangesGuardDialog(
        hasChanges = state.hasChanges,
        onDiscard = { viewModel.onIntent(EditTeaIntent.Discard) },
    ) { onBack ->
        EditTeaContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            snackbarHostState = snackbarHostState,
            // From the route, not state.isEditMode: the state defaults to add mode and only flips once
            // the tea loads, which is a composition too late to decide about the keyboard.
            autoFocusName = route.teaId == null,
        )
    }
}

@Composable
private fun EditTeaContent(
    state: EditTeaState,
    onIntent: (EditTeaIntent) -> Unit,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    autoFocusName: Boolean = false,
) {
    val scope = rememberCoroutineScope()

    val nameFocusRequester = remember { FocusRequester() }
    val originFocusRequester = remember { FocusRequester() }
    val producerFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

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
                    title = { Text(if (state.isEditMode) "Edit Tea" else "Add Tea") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = FeatherIcons.ArrowLeft,
                                contentDescription = stringResource(Res.string.navigate_up),
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
                temperatureCelsius = state.defaultTemperatureCelsius,
                temperatureUnit = state.userPreferences.temperatureUnit,
                temperatureError = state.temperatureError,
                onOriginChanged = { onIntent(EditTeaIntent.OriginChanged(it)) },
                onProducerChanged = { onIntent(EditTeaIntent.ProducerChanged(it)) },
                onPurchaseDateChanged = { onIntent(EditTeaIntent.PurchaseDateChanged(it)) },
                onDescriptionChanged = { onIntent(EditTeaIntent.DescriptionChanged(it)) },
                onTemperatureChanged = { onIntent(EditTeaIntent.TemperatureChanged(it)) },
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
                defaultTemperatureCelsius = 80.0,
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
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
    TeaType(
        id = "black",
        name = "Black",
        defaultTemperatureCelsius = 95.0,
        colorHex = "#795548",
        isSystemDefault = true,
        displayOrder = 1,
        createdAt = PreviewInstant,
        updatedAt = PreviewInstant,
    ),
)
