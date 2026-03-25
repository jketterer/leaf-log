package dev.jketterer.leaflog.presentation.ui.screens.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jketterer.leaflog.domain.repositories.BrewingConfigurationRepository
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import dev.jketterer.leaflog.domain.repositories.TeaRepository
import dev.jketterer.leaflog.domain.usecases.configuration.CreateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.configuration.UpdateBrewingConfigurationUseCase
import dev.jketterer.leaflog.domain.usecases.session.GetBrewingParametersPrefillUseCase
import dev.jketterer.leaflog.presentation.ui.viewmodel.loadPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            is CreateBrewingConfigurationIntent.LoadData -> loadData(intent.teaId, intent.configurationId)
            is CreateBrewingConfigurationIntent.SelectVessel -> {
                val vessel = _state.value.vessels.firstOrNull { it.id == intent.vesselId }
                _state.update { it.copy(selectedVessel = vessel) }
            }

            is CreateBrewingConfigurationIntent.UpdateLabel -> {
                _state.update { it.copy(label = intent.label) }
            }

            is CreateBrewingConfigurationIntent.UpdateTeaQuantity -> {
                _state.update { it.copy(teaQuantityGrams = intent.quantity) }
            }

            is CreateBrewingConfigurationIntent.UpdateWaterQuantity -> updateWaterQuantity(intent.quantity)
            is CreateBrewingConfigurationIntent.UpdateTemperature -> updateTemperature(intent.temperature)
            is CreateBrewingConfigurationIntent.UpdateBrewingTime -> {
                _state.update { it.copy(brewingTime = intent.duration) }
            }

            is CreateBrewingConfigurationIntent.SelectWaterType -> {
                _state.update { it.copy(waterType = intent.waterType) }
            }

            is CreateBrewingConfigurationIntent.ToggleTemperatureUnit -> toggleTemperatureUnit()
            is CreateBrewingConfigurationIntent.ToggleVolumeUnit -> toggleVolumeUnit()
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
                            teaQuantityGrams = config.teaQuantityGrams?.toString() ?: "",
                            waterQuantityMl = config.waterQuantityMl.toString(),
                            waterQuantityDisplay = "",
                            temperatureCelsius = config.temperatureCelsius.toString(),
                            temperatureDisplay = "",
                            brewingTime = config.brewingTime,
                            waterType = config.waterType,
                            isEditMode = true,
                            editingConfigId = configurationId,
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Configuration not found") }
                }
            } else {
                val tea = teaRepository.getByIdFlow(teaId).first()
                val firstVessel = vessels.firstOrNull()

                var prefillTemp = ""
                var prefillWater = ""
                var prefillTime: kotlin.time.Duration? = null
                var prefillWaterType = null as dev.jketterer.leaflog.domain.models.WaterType?
                var prefillTeaQty = ""

                if (tea != null && firstVessel != null) {
                    val prefill = getBrewingParametersPrefillUseCase(tea, firstVessel)
                    prefillTemp = prefill.temperatureCelsius?.toString() ?: ""
                    prefillWater = prefill.waterQuantityMl?.toString() ?: ""
                    prefillTime = prefill.brewingTime
                    prefillWaterType = prefill.waterType
                    prefillTeaQty = prefill.teaQuantityGrams?.toString() ?: ""
                } else if (tea != null) {
                    prefillTemp = tea.defaultTemperatureCelsius?.toDouble()?.toString() ?: ""
                }

                _state.update {
                    it.copy(
                        vessels = vessels,
                        selectedVessel = firstVessel,
                        temperatureCelsius = prefillTemp,
                        temperatureDisplay = "",
                        waterQuantityMl = prefillWater,
                        waterQuantityDisplay = "",
                        brewingTime = prefillTime,
                        waterType = prefillWaterType
                            ?: dev.jketterer.leaflog.domain.models.WaterType.FILTERED,
                        teaQuantityGrams = prefillTeaQty,
                        isEditMode = false,
                        editingConfigId = null,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun updateWaterQuantity(quantity: String) {
        val volumeUnit = _state.value.userPreferences.volumeUnit
        val storageValue = quantity.toIntOrNull()?.let {
            volumeUnit.toMilliliters(it).toString()
        } ?: quantity

        _state.update {
            it.copy(
                waterQuantityMl = storageValue,
                waterQuantityDisplay = quantity,
            )
        }
    }

    private fun updateTemperature(temperature: String) {
        val tempUnit = _state.value.userPreferences.temperatureUnit
        val storageValue = temperature.toIntOrNull()?.let {
            tempUnit.toCelsius(it).toString()
        } ?: temperature

        _state.update {
            it.copy(
                temperatureCelsius = storageValue,
                temperatureDisplay = temperature,
            )
        }
    }

    private fun toggleTemperatureUnit() {
        viewModelScope.launch {
            preferencesRepository.updateTemperatureUnit(_state.value.userPreferences.temperatureUnit.toggle())
            _state.update { it.copy(temperatureDisplay = "") }
        }
    }

    private fun toggleVolumeUnit() {
        viewModelScope.launch {
            preferencesRepository.updateVolumeUnit(_state.value.userPreferences.volumeUnit.toggle())
            _state.update { it.copy(waterQuantityDisplay = "") }
        }
    }

    private fun save() {
        val currentState = _state.value
        val vessel = currentState.selectedVessel ?: return
        val teaId = loadedTeaId ?: return
        val temperatureCelsius = currentState.temperatureCelsius.toDoubleOrNull() ?: return
        val waterQuantityMl = currentState.waterQuantityMl.toDoubleOrNull() ?: return
        val brewingTime = currentState.brewingTime ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            if (currentState.isEditMode && currentState.editingConfigId != null) {
                updateBrewingConfigurationUseCase(
                    configurationId = currentState.editingConfigId,
                    label = currentState.label.takeIf { it.isNotBlank() },
                    vesselId = vessel.id,
                    teaQuantityGrams = currentState.teaQuantityGrams.toFloatOrNull(),
                    waterQuantityMl = waterQuantityMl,
                    temperatureCelsius = temperatureCelsius,
                    brewingTime = brewingTime,
                    waterType = currentState.waterType,
                    isActive = null,
                ).onSuccess {
                    _navEvents.send(CreateBrewingConfigurationNavigationEvent.NavigateBack)
                }.onFailure { e ->
                    _state.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
                }
            } else {
                createBrewingConfigurationUseCase(
                    teaId = teaId,
                    vesselId = vessel.id,
                    teaQuantityGrams = currentState.teaQuantityGrams.toFloatOrNull(),
                    waterQuantityMl = waterQuantityMl,
                    temperatureCelsius = temperatureCelsius,
                    brewingTime = brewingTime,
                    waterType = currentState.waterType,
                    customLabel = currentState.label.takeIf { it.isNotBlank() },
                ).onSuccess {
                    _navEvents.send(CreateBrewingConfigurationNavigationEvent.NavigateBack)
                }.onFailure { e ->
                    _state.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
                }
            }
        }
    }
}

sealed interface CreateBrewingConfigurationNavigationEvent {
    data object NavigateBack : CreateBrewingConfigurationNavigationEvent
}
