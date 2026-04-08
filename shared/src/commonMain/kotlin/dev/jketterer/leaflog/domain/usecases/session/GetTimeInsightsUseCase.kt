package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.TimeInsights
import dev.jketterer.leaflog.domain.models.TimeOfDay
import dev.jketterer.leaflog.domain.repositories.TeaSessionRepository
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetTimeInsightsUseCase(
    private val teaSessionRepository: TeaSessionRepository,
) {
    suspend operator fun invoke(start: Instant, end: Instant): TimeInsights {
        val tz = TimeZone.currentSystemDefault()
        val sessions = teaSessionRepository.getByDateRange(start, end).filter {
            it.status == SessionStatus.COMPLETED && it.parentSessionId == null
        }

        val count = sessions.size
        if (count < 5) return TimeInsights(null, null, count)

        val timeBuckets = mutableMapOf<TimeOfDay, Int>()
        val dayBuckets = mutableMapOf<DayOfWeek, Int>()

        for (session in sessions) {
            val ldt = session.timestamp.toLocalDateTime(tz)
            val tod = when (ldt.hour) {
                in 5..11 -> TimeOfDay.MORNING
                in 12..16 -> TimeOfDay.AFTERNOON
                in 17..21 -> TimeOfDay.EVENING
                else -> TimeOfDay.NIGHT
            }
            timeBuckets[tod] = (timeBuckets[tod] ?: 0) + 1
            dayBuckets[ldt.date.dayOfWeek] = (dayBuckets[ldt.date.dayOfWeek] ?: 0) + 1
        }

        val dominantEntry = timeBuckets.maxByOrNull { it.value }
        val dominantTimeOfDay = dominantEntry?.takeIf { it.value >= count * 0.5 }?.key

        val peakDayEntry = dayBuckets.maxByOrNull { it.value }
        val averagePerDay = count.toFloat() / 7f
        val peakDayOfWeek = peakDayEntry?.takeIf { it.value >= averagePerDay * 2 && it.value >= 3 }?.key

        return TimeInsights(
            dominantTimeOfDay = dominantTimeOfDay,
            peakDayOfWeek = peakDayOfWeek,
            sessionCount = count,
        )
    }
}
