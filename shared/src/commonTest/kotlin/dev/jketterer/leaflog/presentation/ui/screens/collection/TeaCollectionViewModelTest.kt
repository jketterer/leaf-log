package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSortOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class TeaCollectionViewModelTest {

    @Test
    fun `finished teas come last and keep the chosen order among themselves`() {
        val teas = listOf(
            tea("Assam", finished = true),
            tea("Sencha"),
            tea("Bancha", finished = true),
            tea("Keemun"),
        )

        val sorted = sortTeas(teas, TeaSortOption.NAME_ASC)

        assertEquals(listOf("Keemun", "Sencha", "Assam", "Bancha"), sorted.map { it.name })
    }

    @Test
    fun `search combines with the filter and reads producer origin and description`() {
        val teas = listOf(
            tea("Shui Xian", typeId = "oolong", origin = "Wǔyí Shān"),
            tea("Lapsang", typeId = "black", origin = "Wuyi"),
            tea("Dan Cong", typeId = "oolong", description = "Honey orchid"),
        )

        val wuyiOolongs = teas.filteredBy(FilterType.BY_TYPE, "oolong", "wuyi")
        val orchid = teas.filteredBy(FilterType.ALL, null, "orchid")

        assertEquals(listOf("Shui Xian"), wuyiOolongs.map { it.name })
        assertEquals(listOf("Dan Cong"), orchid.map { it.name })
    }

    @Test
    fun `not brewed lately keeps dormant teas longest unbrewed first`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")
        val teas = listOf(
            tea("Sencha", lastBrewedAt = now - 70.days),
            tea("Assam", lastBrewedAt = now - 5.days),
            tea("Keemun", lastBrewedAt = now - 300.days),
            tea("Bancha"),
        )

        assertEquals(listOf("Keemun", "Sencha"), dormantTeas(teas, now).map { it.name })
    }

    private fun tea(
        name: String,
        finished: Boolean = false,
        lastBrewedAt: Instant? = null,
        typeId: String = "type",
        origin: String? = null,
        description: String? = null,
    ) = Tea(
        id = name,
        name = name,
        teaTypeId = typeId,
        origin = origin,
        description = description,
        lastBrewedAt = lastBrewedAt,
        finishedAt = Instant.fromEpochMilliseconds(1).takeIf { finished },
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
}
