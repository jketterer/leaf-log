package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

import kotlin.time.Duration

sealed interface SessionReviewIntent {
    data class NotesChanged(val notes: String) : SessionReviewIntent
    data class PhotoAdded(val imageBytes: ByteArray) : SessionReviewIntent
    data class PhotoRemoved(val path: String) : SessionReviewIntent
    data class SteepClicked(val steepId: String) : SessionReviewIntent
    data class SteepRatingChanged(val rating: Float) : SessionReviewIntent
    data class SteepNotesChanged(val notes: String) : SessionReviewIntent
    data class SteepTagToggled(val tagId: String) : SessionReviewIntent
    data class SteepTagCreated(val name: String) : SessionReviewIntent
    data class SteepParametersChanged(
        val brewedTime: Duration,
        val temperatureCelsius: Double,
        val waterQuantityMl: Double,
    ) : SessionReviewIntent
    data object DismissSteepEdit : SessionReviewIntent
    data object Done : SessionReviewIntent
}
