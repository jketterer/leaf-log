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

    @Test
    fun `storage under a megabyte is in kilobytes rounded up`() {
        assertEquals("0 KB", formatStorageSize(0))
        assertEquals("1 KB", formatStorageSize(1))
        assertEquals("999 KB", formatStorageSize(999_000))
    }

    @Test
    fun `storage under a gigabyte is in whole megabytes`() {
        assertEquals("1 MB", formatStorageSize(999_001))
        assertEquals("999 MB", formatStorageSize(999_499_999))
        assertEquals("85 MB", formatStorageSize(84_600_000))
    }

    @Test
    fun `storage from a gigabyte is in tenths of a gigabyte`() {
        assertEquals("1.0 GB", formatStorageSize(999_500_000))
        assertEquals("2.5 GB", formatStorageSize(2_460_000_000))
    }
}
