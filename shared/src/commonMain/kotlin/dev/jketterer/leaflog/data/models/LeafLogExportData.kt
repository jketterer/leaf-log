package dev.jketterer.leaflog.data.models

import dev.jketterer.leaflog.data.local.database.entities.BrewingConfigurationEntity
import dev.jketterer.leaflog.data.local.database.entities.BrewingVesselEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaSessionEntity
import dev.jketterer.leaflog.data.local.database.entities.TeaTypeEntity
import kotlinx.serialization.Serializable

@Serializable
data class LeafLogExportData(
    val version: Int = 1,
    val exportedAt: Long,
    val teaTypes: List<TeaTypeEntity>,
    val brewingVessels: List<BrewingVesselEntity>,
    val teas: List<TeaEntity>,
    val teaSessions: List<TeaSessionEntity>,
    val brewingConfigurations: List<BrewingConfigurationEntity>,
)
