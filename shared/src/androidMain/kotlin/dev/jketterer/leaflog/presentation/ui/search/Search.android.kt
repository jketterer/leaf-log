package dev.jketterer.leaflog.presentation.ui.search

import java.text.Normalizer

private val combiningMarks = Regex("\\p{Mn}+")

internal actual fun String.foldedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()
