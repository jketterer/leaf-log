package dev.jketterer.leaflog.presentation.ui.screens.sessionreview

sealed interface SessionReviewIntent {
    data class NotesChanged(val notes: String) : SessionReviewIntent
    data class PhotoAdded(val imageBytes: ByteArray) : SessionReviewIntent
    data class PhotoRemoved(val path: String) : SessionReviewIntent
    data class SteepClicked(val steepId: String) : SessionReviewIntent
    data class SteepRatingChanged(val rating: Float) : SessionReviewIntent
    data class SteepNotesChanged(val notes: String) : SessionReviewIntent
    data object DismissSteepEdit : SessionReviewIntent
    data class MethodLabelChanged(val label: String) : SessionReviewIntent
    data object SaveMethod : SessionReviewIntent
    data object UpdateMethod : SessionReviewIntent
    data object Done : SessionReviewIntent
}
