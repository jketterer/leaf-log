package dev.jketterer.leaflog.domain.models

import java.util.Locale
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // 36 needs JVM module flags on JDK 21
class TimeFormatterTest {

    private val savedLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(savedLocale)

    private fun format(
        skeleton: DateSkeleton,
        locale: Locale,
        date: LocalDate = LocalDate(2024, 1, 15),
    ): String {
        Locale.setDefault(locale)
        return TimeFormatter.formatDate(date, skeleton)
    }

    @Test
    fun `every skeleton formats in US English`() {
        assertEquals(
            listOf("Jan", "Jan 15", "1/15", "Jan 2024", "Jan 15, 2024", "Monday, January 15"),
            DateSkeleton.entries.map { format(it, Locale.US) },
        )
    }

    @Test
    fun `British English puts the day first`() {
        assertEquals("15 Jan 2024", format(DateSkeleton.MONTH_DAY_YEAR, Locale.UK))
    }

    @Test
    fun `German uses its own month names and punctuation`() {
        assertEquals(
            "15. März",
            format(DateSkeleton.MONTH_DAY, Locale.GERMANY, date = LocalDate(2024, 3, 15)),
        )
    }
}
