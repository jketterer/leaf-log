package dev.jketterer.leaflog.domain.models

data class SteepInsights(
    val averageSteepsPerSession: Float,
    val topReSteepedTea: TopReSteepedTea?,
    val newTeaDiscoveries: Int,
)

data class TopReSteepedTea(val tea: Tea, val averageSteeps: Float)
