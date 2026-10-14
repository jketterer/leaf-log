package dev.jketterer.leaflog.presentation.ui.screens.collection

import dev.jketterer.leaflog.domain.models.Tea
import dev.jketterer.leaflog.domain.models.TeaSortOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SortTeasTest {

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

    private fun tea(name: String, finished: Boolean = false) = Tea(
        id = name,
        name = name,
        teaTypeId = "type",
        finishedAt = Instant.fromEpochMilliseconds(1).takeIf { finished },
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
}
