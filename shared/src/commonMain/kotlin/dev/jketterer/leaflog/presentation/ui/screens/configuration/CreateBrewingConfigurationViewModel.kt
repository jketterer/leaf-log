package dev.jketterer.leaflog.presentation.ui.screens.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.presentation.ui.messages.UiMessages
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.couldnt_save_method
import leaf_log.shared.generated.resources.failure_not_found

class CreateBrewingConfigurationViewModel(
    private val teaRepository: TeaRepository,
    private val brewingVesselRepository: BrewingVesselRepository,
    private val brewingConfigurationRepository: BrewingConfigurationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getBrewingParametersPrefillUseCase: GetBrewingParametersPrefillUseCase,
    private val createBrewingConfigurationUseCase: CreateBrewingConfigurationUseCase,
    private val updateBrewingConfigurationUseCase: UpdateBrewingConfigurationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateBrewingConfigurationState(isLoading = true))
    val state: StateFlow<CreateBrewingConfigurationState> = _state.asStateFlow()

    val messages = UiMessages<Nothing>()

    private val _navEvents = Channel<CreateBrewingConfigurationNavigationEvent>()
    val navEvents = _navEvents.receiveAsFlow()

    private var loadedTeaId: String? = null
    private var loadedConfigId: String? = null

    init {
        loadPreferences(
            preferencesRepository = preferencesRepository,
            stateFlow = _state,
            updateState = { state, prefs -> state.copy(userPreferences = prefs) }
        )
    }

    fun onIntent(intent: CreateBrewingConfigurationIntent) {
        when (intent) {
            is CreateBrewingConfigurationIntent.LoadData -> loadData(
                intent.teaId,
                intent.configurationId
            )

            is CreateBrewingConfigurationIntent.SelectVessel -> {
                val vessel = _state.value.vessels.firstOrNull { it.id == intent.vesselId }
                _state.update { it.copy(selectedVessel = vessel) }
            }

            is CreateBrewingConfigurationIntent.UpdateLabel -> {
                _state.update { it.copy(label = intent.label) }
            }

            is CreateBrewingConfigurationIntent.UpdateTeaQuantity -> {
                _state.update { it.copy(teaQuantityGrams = intent.grams) }
            }

            is CreateBrewingConfigurationIntent.UpdateWaterQuantity -> {
                _state.update { it.copy(waterQuantityMl = intent.milliliters) }
            }

            is CreateBrewingConfigurationIntent.UpdateTemperature -> {
                _state.update { it.copy(temperatureCelsius = intent.celsius) }
            }

            is CreateBrewingConfigurationIntent.UpdateBrewingTime -> {
                _state.update { it.copy(brewingTime = intent.duration) }
            }

            is CreateBrewingConfigurationIntent.SelectWaterType -> {
                _state.update { it.copy(waterType = intent.waterType) }
            }

            is CreateBrewingConfigurationIntent.Save -> save()
            is CreateBrewingConfigurationIntent.NavigateBack -> {
                _navEvents.trySend(CreateBrewingConfigurationNavigationEvent.NavigateBack)
            }
        }
    }

    private fun loadData(teaId: String, configurationId: String?) {
        if (loadedTeaId == teaId && loadedConfigId == configurationId) return
        loadedTeaId = teaId
        loadedConfigId = configurationId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val vessels = brewingVesselRepository.getActiveFlow().first()

            if (configurationId != null) {
                val config = brewingConfigurationRepository.getById(configurationId)
                if (config != null) {
                    val selectedVessel = vessels.firstOrNull { it.id == config.vesselId }
                    _state.update {
                        it.copy(
                            vessels = vessels,
                            selectedVessel = selectedVessel,
                            label = config.label ?: "",
                            teaQuantityGrams = config.teaQuantityGrams?.toDouble(),
                            waterQuantityMl = config.waterQuantityMl,
                            temperatureCelsius = config.temperatureCelsius,
                            brewingTime = config.brewingTime,
                            waterType = config.waterType,
                            isEditMode = true,
                            editingConfigId = configurationId,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    messages.show(Res.string.failure_not_found)
                }
            } else {
                val tea = teaRepository.getByIdFlow(teaId).first()
                val firstVessel = vessels.firstOrNull()

                var prefillTemp: Double? = null
                var prefillWater: Double? = null
                var prefillTime: kotlin.time.Duration? = null
                var prefillWaterType = null as WaterType?
                var prefillTeaQty: Double? = null

                if (tea != null && firstVessel != null) {
                    val prefill = getBrewingParametersPrefillUseCase(tea, firstVessel)
                    prefillTemp = prefill.temperatureCelsius
                    prefillWater = prefill.waterQuantityMl
                    prefillTime = prefill.brewingTime
                    prefillWaterType = prefill.waterType
                    prefillTeaQty = prefill.teaQuantityGrams?.toDouble()
                } else if (tea != null) {
                    prefillTemp = tea.defaultTemperatureCelsius
                }

                _state.update {
                    it.copy(
                        vessels = vessels,
                        selectedVessel = firstVessel,
                        temperatureCelsius = prefillTemp,
                        waterQuantityMl = prefillWater,
                        brewingTime = prefillTime,
                        waterType = prefillWaterType ?: WaterType.FILTERED,
                        teaQuantityGrams = prefillTeaQty,
                        isEditMode = false,
                        editingConfigId = null,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun save() {
        val currentState = _state.value
        val vessel = currentState.selectedVessel ?: return
        val teaId = loadedTeaId ?: return
        val temperatureCelsius = currentState.temperatureCelsius ?: return
        val waterQuantityMl = currentState.waterQuantityMl ?: return
        val brewingTime = currentState.brewingTime ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            if (currentState.isEditMode && currentState.editingConfigId != null) {
                updateBrewingConfigurationUseCase(
                    configurationId = currentState.editingConfigId,
                    label = currentState.label.takeIf { it.isNotBlank() },
                    vesselId = vessel.id,
                    teaQuantityGrams = currentState.teaQuantityGrams?.toFloat(),
                    waterQuantityMl = waterQuantityMl,
                    temperatureCelsius = temperatureCelsius,
                    brewingTime = brewingTime,
                    waterType = currentState.waterType,
                ).onSuccess {
                    _navEvents.send(CreateBrewingConfigurationNavigationEvent.NavigateBack)
                }.onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_method)
                }
            } else {
                createBrewingConfigurationUseCase(
                    teaId = teaId,
                    vesselId = vessel.id,
                    teaQuantityGrams = currentState.teaQuantityGrams?.toFloat(),
                    waterQuantityMl = waterQuantityMl,
                    temperatureCelsius = temperatureCelsius,
                    brewingTime = brewingTime,
                    waterType = currentState.waterType,
                    customLabel = currentState.label.takeIf { it.isNotBlank() },
                ).onSuccess {
                    _navEvents.send(CreateBrewingConfigurationNavigationEvent.NavigateBack)
                }.onFailure { e ->
                    _state.update { it.copy(isSaving = false) }
                    messages.failure(e, Res.string.couldnt_save_method)
                }
            }
        }
    }
}

sealed interface CreateBrewingConfigurationNavigationEvent {
    data object NavigateBack : CreateBrewingConfigurationNavigationEvent
}
