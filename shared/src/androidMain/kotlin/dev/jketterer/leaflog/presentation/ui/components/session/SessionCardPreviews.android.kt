package dev.jketterer.leaflog.presentation.ui.components.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.data.local.ImageStorage
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.presentation.ui.components.common.previewSession
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Preview
@Composable
private fun SessionCardWithPhotoPreview() {
    val imageStorage = ImageStorage(LocalContext.current)
    LeafLogTheme {
        SessionCard(
            session = previewSession(
                ratings = listOf(4f),
                status = SessionStatus.COMPLETED,
                temperatureCelsius = 85.0,
                notes = "Lovely floral aroma, light and refreshing",
                photos = listOf("https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Tea_in_different_grade_of_fermentation.jpg/320px-Tea_in_different_grade_of_fermentation.jpg"),
            ),
            teaName = "Jasmine Green Tea",
            teaTypeName = "Green Tea",
            vesselName = "Gaiwan",
            teaPhotoUrl = null,
            onSessionClick = {},
            onDeleteClick = {},
            onBrewAgainClick = {},
            imageStorage = imageStorage,
        )
    }
}
