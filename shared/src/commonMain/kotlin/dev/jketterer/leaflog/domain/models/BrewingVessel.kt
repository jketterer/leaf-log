package dev.jketterer.leaflog.domain.models

import kotlin.time.Instant

data class BrewingVessel(
    val id: String,
    val name: String,
    val iconName: String?,
    val imagePath: String? = null,
    val capacityMl: Int? = null,
    val isSystemDefault: Boolean,
    val isArchived: Boolean = false,
    val displayOrder: Int,

    val userId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
/** A tea brewed in a vessel: how many of its completed sessions, and the last one's start. */
data class VesselTea(
    val tea: Tea,
    val teaType: TeaType?,
    val sessions: Int,
    val lastBrewed: Instant,
)

/** What a vessel has brewed, from its completed sessions. */
data class VesselProfile(
    val sessions: Int,
    val lastBrewed: Instant,
    /** Tea types by session count, most first. */
    val types: List<TeaTypeDistribution>,
    /** The type it's kept for: one type in [DEDICATED_SHARE] of [DEDICATED_AFTER] or more sessions. */
    val dominantType: TeaType?,
    /** Whether [dominantType] is every session's, not only most. */
    val onlyType: Boolean,
    /** The teas brewed in it, the one brewed most recently first. */
    val teas: List<VesselTea>,
)

/** Sessions a vessel needs before one type can be called what it's kept for. */
const val DEDICATED_AFTER = 3

/** The share of a vessel's sessions one type needs to be what it's kept for. */
const val DEDICATED_SHARE = 0.75f

/** The profile of the vessel these sessions were brewed in; null when none is completed. */
fun List<SessionWithSteeps>.vesselProfile(
    teas: Map<String, Tea>,
    teaTypes: Map<String, TeaType>,
): VesselProfile? {
    val completed = filter { it.session.status == SessionStatus.COMPLETED }
    if (completed.isEmpty()) return null
    val types = completed.mapNotNull { teas[it.session.teaId]?.teaTypeId }
        .groupingBy { it }.eachCount()
        .mapNotNull { (typeId, count) ->
            teaTypes[typeId]?.let { TeaTypeDistribution(it, count, count * 100f / completed.size) }
        }
        .sortedByDescending { it.sessionCount }
    val dominant = types.firstOrNull()?.takeIf {
        completed.size >= DEDICATED_AFTER && it.sessionCount >= completed.size * DEDICATED_SHARE
    }
    return VesselProfile(
        sessions = completed.size,
        lastBrewed = completed.maxOf { it.startedAt },
        types = types,
        dominantType = dominant?.teaType,
        onlyType = dominant?.sessionCount == completed.size,
        teas = completed.groupBy { it.session.teaId }
            .mapNotNull { (teaId, sessions) ->
                teas[teaId]?.let { tea ->
                    VesselTea(tea, teaTypes[tea.teaTypeId], sessions.size, sessions.maxOf { it.startedAt })
                }
            }
            .sortedByDescending { it.lastBrewed },
    )
}
