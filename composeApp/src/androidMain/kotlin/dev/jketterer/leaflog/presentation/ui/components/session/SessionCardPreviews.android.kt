package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.SyncStatus
import dev.jketterer.leaflog.domain.models.TeaSession
import dev.jketterer.leaflog.domain.models.UserPreferences
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@Preview
@Composable
private fun SessionCardWithPhotoPreview() {
    val imageStorage = ImageStorage(LocalContext.current)
    LeafLogTheme {
        SessionCard(
            session = TeaSession(
                id = "0",
                teaId = "tea-0",
                steepNumber = 1,
                vesselId = "vessel-0",
                waterType = WaterType.FILTERED,
                timestamp = Instant.fromEpochMilliseconds(1735747200000),
                status = SessionStatus.COMPLETED,
                brewingTime = 180.toDuration(DurationUnit.SECONDS),
                updatedAt = Instant.fromEpochMilliseconds(1735747200000),
                deletedAt = null,
                createdAt = Instant.fromEpochMilliseconds(1735747200000),
                teaQuantityGrams = 5f,
                temperatureCelsius = 85.0,
                waterQuantityMl = 200.0,
                photos = listOf("https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Tea_in_different_grade_of_fermentation.jpg/320px-Tea_in_different_grade_of_fermentation.jpg"),
                syncStatus = SyncStatus.LOCAL_ONLY,
                rating = 4.0f,
                notes = "Lovely floral aroma, light and refreshing",
            ),
            teaName = "Jasmine Green Tea",
            teaTypeName = "Green Tea",
            vesselName = "Gaiwan",
            teaPhotoUrl = null,
            userPrefs = UserPreferences.METRIC,
            onSessionClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = imageStorage,
        )
    }
}
