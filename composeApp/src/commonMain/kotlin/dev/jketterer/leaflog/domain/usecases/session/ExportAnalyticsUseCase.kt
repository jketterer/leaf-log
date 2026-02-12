package dev.jketterer.leaflog.domain.usecases.session

import dev.jketterer.leaflog.domain.models.AnalyticsData
import dev.jketterer.leaflog.domain.models.TeaTypeDistribution
import dev.jketterer.leaflog.domain.models.TopTea
import dev.jketterer.leaflog.domain.models.TrendPoint

class ExportAnalyticsUseCase {
    operator fun invoke(
        analytics: AnalyticsData,
        trendPoints: List<TrendPoint>,
        teaTypeDistribution: List<TeaTypeDistribution>,
        topTeas: List<TopTea>,
        periodLabel: String,
    ): String = buildString {
        appendLine("Leaf Log Analytics Export - $periodLabel")
        appendLine()

        // Summary section
        appendLine("Summary")
        appendLine("Total Sessions,${analytics.totalSessions}")
        appendLine("Total Brewing Time (min),${analytics.totalBrewingTime.inWholeMinutes}")
        appendLine("Total Water (mL),${analytics.totalWaterMl}")
        appendLine("Unique Teas,${analytics.uniqueTeasCount}")
        val avgRating = analytics.averageRating?.let { "%.1f".format(it) } ?: "N/A"
        appendLine("Average Rating,$avgRating")
        appendLine("Rated Sessions,${analytics.ratedSessionsCount}")
        appendLine()

        // Daily trends
        if (trendPoints.isNotEmpty()) {
            appendLine("Daily Brewing Trends")
            appendLine("Date,Sessions")
            trendPoints.forEach { point ->
                appendLine("${point.date},${point.sessionCount}")
            }
            appendLine()
        }

        // Tea type distribution
        if (teaTypeDistribution.isNotEmpty()) {
            appendLine("Tea Type Distribution")
            appendLine("Tea Type,Sessions,Percentage")
            teaTypeDistribution.forEach { dist ->
                appendLine("${dist.teaType.name},${dist.sessionCount},${"%.1f".format(dist.percentage)}%")
            }
            appendLine()
        }

        // Top teas
        if (topTeas.isNotEmpty()) {
            appendLine("Top Teas")
            appendLine("Tea,Sessions")
            topTeas.forEach { top ->
                appendLine("${top.tea.name},${top.sessionCount}")
            }
        }
    }
}
