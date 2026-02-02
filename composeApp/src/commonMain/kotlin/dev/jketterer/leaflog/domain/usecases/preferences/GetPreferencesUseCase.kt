package dev.jketterer.leaflog.domain.usecases.preferences

import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.repositories.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<UserPreferences> {
        return preferencesRepository.getPreferencesFlow()
    }
}
