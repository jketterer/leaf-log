package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TastingTagTest {

    @Test
    fun `tapping a carried-over tag confirms it`() {
        val tags = listOf(SteepTag("floral", confirmed = false), SteepTag("sweet", confirmed = false))

        assertEquals(
            listOf(SteepTag("floral", confirmed = true), SteepTag("sweet", confirmed = false)),
            tags.toggled("floral"),
        )
    }

    @Test
    fun `tapping a confirmed tag removes it`() {
        val tags = listOf(SteepTag("floral", confirmed = true), SteepTag("sweet", confirmed = false))

        assertEquals(listOf(SteepTag("sweet", confirmed = false)), tags.toggled("floral"))
    }

    @Test
    fun `tapping a tag the steep doesn't have adds it confirmed`() {
        assertEquals(listOf(SteepTag("floral", confirmed = true)), emptyList<SteepTag>().toggled("floral"))
    }

    @Test
    fun `the curated vocabulary is 23 distinct tags`() {
        assertEquals(23, CURATED_TAG_KEYS.toSet().size)
    }

    @Test
    fun `dominant tags rank by how many steeps confirmed each`() {
        val session = session(
            listOf(SteepTag("sweet", true), SteepTag("floral", true)),
            listOf(SteepTag("sweet", true), SteepTag("floral", false)),
            listOf(SteepTag("sweet", true), SteepTag("bitter", true)),
        )

        assertEquals(listOf("sweet", "floral", "bitter"), session.dominantTags(vocabulary).map { it.id })
    }

    @Test
    fun `dominant tags break ties in vocabulary order`() {
        val session = session(listOf(SteepTag("bitter", true), SteepTag("sweet", true), SteepTag("floral", true)))

        assertEquals(listOf("floral", "sweet", "bitter"), session.dominantTags(vocabulary).map { it.id })
    }

    @Test
    fun `an untouched carry-over is not a dominant tag`() {
        val session = session(listOf(SteepTag("floral", true)), listOf(SteepTag("floral", false), SteepTag("sweet", false)))

        assertEquals(listOf("floral"), session.dominantTags(vocabulary).map { it.id })
    }

    @Test
    fun `dominant tags stop at three`() {
        val session = session(CURATED_TAG_KEYS.map { SteepTag(it, true) })

        assertEquals(listOf("floral", "fruity", "honey"), session.dominantTags(vocabulary).map { it.id })
    }

    private val vocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
        TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
    }

    private fun session(vararg steepTags: List<SteepTag>) = SessionWithSteeps(
        session = Session(
            id = "s1",
            teaId = "tea",
            status = SessionStatus.COMPLETED,
            vesselId = "vessel",
            waterType = WaterType.FILTERED,
            syncStatus = SyncStatus.LOCAL_ONLY,
        ),
        steeps = steepTags.mapIndexed { i, tags ->
            Steep(
                id = "s${i + 1}",
                number = i + 1,
                timestamp = Instant.fromEpochSeconds(0),
                brewingTime = 20.seconds,
                temperatureCelsius = 95.0,
                waterQuantityMl = 100.0,
                tags = tags,
                createdAt = Instant.fromEpochSeconds(0),
                updatedAt = Instant.fromEpochSeconds(0),
            )
        },
    )
}
