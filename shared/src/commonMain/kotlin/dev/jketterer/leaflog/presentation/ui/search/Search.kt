package dev.jketterer.leaflog.presentation.ui.search

/**
 * Whether every word of [query] appears in some field, ignoring case and accents. Words can match
 * different fields, and a blank query matches everything.
 */
fun matchesQuery(query: String, fields: List<String?>): Boolean {
    val words = query.foldedForSearch().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val folded = fields.mapNotNull { it?.foldedForSearch() }
    return words.all { word -> folded.any { it.contains(word) } }
}

/** Lowercased with its accents stripped, so "Tiě Guān Yīn" reads as "tie guan yin". */
internal expect fun String.foldedForSearch(): String
