package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.ActivityCell
import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.AnalyticsPeriod
import dev.jketterer.leaflog.domain.models.Insight
import dev.jketterer.leaflog.domain.models.InsightType
import dev.jketterer.leaflog.domain.models.SteepInsights
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopRatedTea
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.VesselDistribution
import kotlin.math.roundToInt

class GenerateInsightsUseCase {
    operator fun invoke(
        current: AnalyticsData,
        topTeas: List<TopTea>,
        steepInsights: SteepInsights?,
        teaTypeDistribution: List<TeaTypeDistribution>,
        topRatedTeas: List<TopRatedTea>,
        vesselDistribution: List<VesselDistribution>,
        activityCells: List<ActivityCell>,
        period: AnalyticsPeriod,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Most brewed tea (only if there's a clear winner — no ties)
        val maxBrewCount = topTeas.maxOfOrNull { it.sessionCount } ?: 0
        val topTeaList = topTeas.filter { it.sessionCount == maxBrewCount }
        if (maxBrewCount > 1 && topTeaList.size == 1) {
            insights.add(
                Insight(
                    type = InsightType.MOST_BREWED,
                    text = "${topTeaList.first().tea.name} is your most brewed tea with ${maxBrewCount} sessions",
                )
            )
        }

        // Average rating
        if (current.ratedSessionsCount >= 5 && current.averageRating != null) {
            val rounded = (current.averageRating * 10).roundToInt() / 10f
            insights.add(
                Insight(
                    type = InsightType.AVERAGE_RATING,
                    text = "Your average rating is $rounded across ${current.ratedSessionsCount} rated sessions",
                )
            )
        }

        // Favorite tea type
        val topType = teaTypeDistribution.maxByOrNull { it.sessionCount }
        if (topType != null && topType.sessionCount > 1) {
            val pct = topType.percentage.roundToInt()
            insights.add(
                Insight(
                    type = InsightType.FAVORITE_TYPE,
                    text = "${topType.teaType.name} tea makes up ${pct}% of your sessions",
                )
            )
        }

        // Most re-steeped tea
        val topReSteep = steepInsights?.topReSteepedTea
        if (topReSteep != null) {
            val avgSteeps = (topReSteep.averageSteeps * 10).roundToInt() / 10.0
            insights.add(
                Insight(
                    type = InsightType.RE_STEEP,
                    text = "${topReSteep.tea.name} is your most re-steeped tea, averaging $avgSteeps steeps per session",
                )
            )
        }

        // Highest rated tea
        val topRated = topRatedTeas.firstOrNull()
        if (topRated != null && topRated.ratedSessionCount >= 2) {
            val rounded = (topRated.averageRating * 10).roundToInt() / 10f
            insights.add(
                Insight(
                    type = InsightType.TOP_RATED,
                    text = "${topRated.tea.name} is your highest rated tea at $rounded",
                )
            )
        }

        // Preferred vessel
        val topVessel = vesselDistribution.maxByOrNull { it.sessionCount }
        if (topVessel != null && vesselDistribution.size > 1) {
            val pct = topVessel.percentage.roundToInt()
            insights.add(
                Insight(
                    type = InsightType.PREFERRED_VESSEL,
                    text = "${topVessel.vessel.name} is your go-to vessel, used in ${pct}% of sessions",
                )
            )
        }

        // Brewing consistency (streak)
        val useWeekly = period == AnalyticsPeriod.LAST_90_DAYS || period == AnalyticsPeriod.THIS_YEAR
        val maxStreak = computeMaxStreak(activityCells.sortedBy { it.date })
        if (maxStreak >= 3) {
            val unit = if (useWeekly) "week" else "day"
            insights.add(
                Insight(
                    type = InsightType.CONSISTENCY,
                    text = "Your longest brewing streak was $maxStreak ${unit}s in a row",
                )
            )
        }

        return insights
    }

    private fun computeMaxStreak(cells: List<ActivityCell>): Int {
        var maxStreak = 0
        var currentStreak = 0
        for (cell in cells) {
            if (cell.sessionCount > 0) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }
        return maxStreak
    }
}
