package dev.jketterer.leaflog.presentation.ui.screens.settings

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsViewModelTest {

    private val tz = TimeZone.of("America/New_York")

    private fun at(iso: String) = LocalDateTime.parse(iso).toInstant(tz)

    private fun daysSince(backup: String, now: String) = daysSinceBackup(at(backup), at(now), tz)

    @Test
    fun `no backup has no age`() {
        assertNull(daysSinceBackup(null, at("2026-10-15T09:00"), tz))
    }

    @Test
    fun `earlier the same day is zero days`() {
        assertEquals(0, daysSince("2026-10-15T00:01", now = "2026-10-15T23:59"))
    }

    @Test
    fun `late last night is one day`() {
        assertEquals(1, daysSince("2026-10-14T23:59", now = "2026-10-15T00:01"))
    }

    @Test
    fun `days are counted across months`() {
        assertEquals(42, daysSince("2026-09-03T12:00", now = "2026-10-15T08:00"))
    }

    @Test
    fun `a backup after now reads as today`() {
        assertEquals(0, daysSince("2026-10-16T09:00", now = "2026-10-15T09:00"))
    }
}
