package dev.jketterer.leaflog.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun `the flavor profile counts a tag once per session however many steeps confirmed it`() {
        val gongfu = session(*Array(10) { listOf(SteepTag("bitter", true)) })
        val western = List(2) { session(listOf(SteepTag("sweet", true))) }

        val profile = (western + gongfu).flavorProfile(vocabulary)!!

        assertEquals(listOf("sweet" to 2, "bitter" to 1), profile.tags.map { (tag, count) -> tag.id to count })
    }

    @Test
    fun `the flavor profile is out of the sessions with a confirmed tag`() {
        val sessions = listOf(
            session(listOf(SteepTag("floral", true))),
            session(listOf(SteepTag("sweet", true))),
            session(listOf(SteepTag("floral", false))),
            session(emptyList()),
        )

        assertEquals(2, sessions.flavorProfile(vocabulary)!!.sessions)
    }

    @Test
    fun `the flavor profile stops at eight`() {
        val profile = listOf(session(CURATED_TAG_KEYS.map { SteepTag(it, true) })).flavorProfile(vocabulary)!!

        assertEquals(CURATED_TAG_KEYS.take(8), profile.tags.map { it.first.id })
    }

    @Test
    fun `no confirmed tags means no flavor profile`() {
        assertNull(listOf(session(listOf(SteepTag("floral", false)))).flavorProfile(vocabulary))
    }

    @Test
    fun `a flavor missing from the previous season's profile is new`() {
        val previous = listOf(session(listOf(SteepTag("floral", true)))).flavorProfile(vocabulary)
        val current = listOf(session(listOf(SteepTag("floral", true), SteepTag("sweet", true)))).flavorProfile(vocabulary)!!

        assertEquals(setOf("sweet"), current.newSince(previous))
    }

    @Test
    fun `without a previous profile no flavor is new`() {
        val current = listOf(session(listOf(SteepTag("floral", true)))).flavorProfile(vocabulary)!!

        assertEquals(emptySet(), current.newSince(null))
    }

    @Test
    fun `a tag's usage ranks the teas that confirmed it most and keeps the latest sessions`() {
        val floral = listOf(SteepTag("floral", true))
        val sessions = listOf(
            session(floral, id = "newest", teaId = "dragon-well"),
            session(listOf(SteepTag("floral", false)), teaId = "dragon-well"),
            session(floral, teaId = "tieguanyin"),
            session(floral, teaId = "tieguanyin"),
            session(listOf(SteepTag("sweet", true)), teaId = "sencha"),
            session(floral, teaId = "sencha", status = SessionStatus.IN_PROGRESS),
        )

        val usage = sessions.usageOf("floral")

        assertEquals(3, usage.sessions)
        assertEquals(4, usage.tagged)
        assertEquals(listOf("tieguanyin" to 2, "dragon-well" to 1), usage.teas)
        assertEquals("newest", usage.recent.first().session.id)
        assertEquals(3, usage.recent.size)
    }

    private val vocabulary = CURATED_TAG_KEYS.mapIndexed { i, key ->
        TastingTag(id = key, key = key, name = null, isCustom = false, displayOrder = i)
    }

    private fun session(
        vararg steepTags: List<SteepTag>,
        id: String = "s1",
        teaId: String = "tea",
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = SessionWithSteeps(
        session = Session(
            id = id,
            teaId = teaId,
            status = status,
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
