package dev.jketterer.leaflog.domain.models

enum class TeaSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    RATING_DESC("Rating"),
    TIMES_BREWED_DESC("Times Brewed"),
    MOST_RECENT("Most Recent"),
    OLDEST("Oldest"),
}
