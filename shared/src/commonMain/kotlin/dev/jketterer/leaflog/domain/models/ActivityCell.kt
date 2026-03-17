package dev.jketterer.leaflog.domain.models

import kotlinx.datetime.LocalDate

data class ActivityCell(
    val date: LocalDate,                       // day start (daily) or Monday (weekly)
    val dominantTeaTypeColorHex: String?,      // null = no sessions that day/week
    val sessionCount: Int = 0,
)
