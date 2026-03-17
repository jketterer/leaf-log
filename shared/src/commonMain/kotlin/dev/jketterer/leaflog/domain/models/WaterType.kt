package dev.jketterer.leaflog.domain.models

enum class WaterType(val displayName: String) {
    FILTERED("Filtered Water"),
    TAP("Tap Water"),
    SPRING("Spring Water"),
    DISTILLED("Distilled Water"),
    MINERAL("Mineral Water"),
    ALKALINE("Alkaline Water"),
    OTHER("Other"),
}