package dev.jketterer.leaflog.presentation.ui.screens.flavors

sealed interface FlavorDetailIntent {
    data object RenameClicked : FlavorDetailIntent
    data object MergeClicked : FlavorDetailIntent
    data object DeleteClicked : FlavorDetailIntent
    data object DialogDismissed : FlavorDetailIntent
    data class RenameConfirmed(val name: String) : FlavorDetailIntent
    /** Chosen in the merge picker, or offered by a rename to a name that's taken. */
    data class MergeTargetChosen(val targetId: String) : FlavorDetailIntent
    data object RemovalConfirmed : FlavorDetailIntent
}

sealed interface FlavorDetailNavigationEvent {
    data object NavigateBack : FlavorDetailNavigationEvent
    /** The tag was merged into [tagId], whose page takes this one's place. */
    data class NavigateToFlavor(val tagId: String) : FlavorDetailNavigationEvent
}
