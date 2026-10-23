package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate

data class ActivityCell(
    val date: LocalDate,                       // the day, or the week's or month's first day in the window
    val dominantTeaTypeColorHex: String?,      // null = no sessions in the cell
    val sessionCount: Int = 0,
)
