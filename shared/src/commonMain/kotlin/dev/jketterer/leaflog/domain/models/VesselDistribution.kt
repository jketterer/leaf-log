package dev.jketterer.leaflog.domain.models

data class VesselDistribution(val vessel: BrewingVessel, val sessionCount: Int, val percentage: Float)
