package dev.jketterer.leaflog.domain.models

data class ImportResult(
    val teaTypesImported: Int,
    val brewingVesselsImported: Int,
    val teasImported: Int,
    val teaSessionsImported: Int,
    val configurationsImported: Int,
)
