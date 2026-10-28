package dev.jketterer.leaflog.flow

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import dev.jketterer.leaflog.domain.models.SessionStatus
import dev.jketterer.leaflog.domain.models.WaterType
import dev.jketterer.leaflog.domain.repositories.BrewingVesselRepository
import dev.jketterer.leaflog.domain.usecases.CreateTeaUseCase
import dev.jketterer.leaflog.domain.usecases.InitializeDefaultsUseCase
import dev.jketterer.leaflog.domain.usecases.session.StartSessionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.analytics_period_all_time
import leaf_log.shared.generated.resources.analytics_period_this_season
import leaf_log.shared.generated.resources.analytics_period_this_week
import leaf_log.shared.generated.resources.analytics_top_teas
import leaf_log.shared.generated.resources.recap_close
import leaf_log.shared.generated.resources.recap_most_brewed
import leaf_log.shared.generated.resources.recap_title_summer
import leaf_log.shared.generated.resources.recaps
import leaf_log.shared.generated.resources.season_label
import leaf_log.shared.generated.resources.season_summer
import leaf_log.shared.generated.resources.tab_analytics
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Analytics and the recaps it opens, over a summer of ten brews, six Dragon Well and four Da Hong
 * Pao, and three in the autumn's first days, seen on 5 September while summer's recap is new.
 */
class AnalyticsFlowTest : FlowTest() {

    @Before
    fun seed() = runBlocking {
        koin.get<InitializeDefaultsUseCase>()()
        val dragonWell = koin.get<CreateTeaUseCase>()(name = "Dragon Well", teaTypeId = "green").getOrThrow().id
        val daHongPao = koin.get<CreateTeaUseCase>()(name = "Da Hong Pao", teaTypeId = "oolong").getOrThrow().id
        val gaiwan = koin.get<BrewingVesselRepository>().getActiveFlow().first().first { it.name == "Gaiwan" }.id
        listOf(
            dragonWell to "06-10", daHongPao to "06-15", dragonWell to "06-20", dragonWell to "07-05",
            daHongPao to "07-10", dragonWell to "07-15", dragonWell to "08-01", daHongPao to "08-05",
            dragonWell to "08-20", daHongPao to "08-25",
            dragonWell to "09-02", daHongPao to "09-03", daHongPao to "09-04",
        ).forEach { (tea, day) ->
            clock.set(Instant.parse("2026-${day}T15:00:00Z"))
            koin.get<StartSessionUseCase>()(
                teaId = tea,
                vesselId = gaiwan,
                waterType = WaterType.FILTERED,
                brewingTime = 30.seconds,
                temperatureCelsius = 90.0,
                waterQuantityMl = 100.0,
                status = SessionStatus.COMPLETED,
            ).getOrThrow()
        }
        clock.set(Instant.parse("2026-09-05T15:00:00Z"))
    }

    @Test
    fun `a recap paged past its cover stays on that page through recreation and a tea opened from it`() {
        openSummerRecap()
        swipeTo(mostBrewed)

        recreate()
        waitFor(mostBrewed)

        click(hasClickAction() and hasText(mostBrewed))
        waitFor("Edit tea")
        systemBack()
        waitFor(mostBrewed)
    }

    @Test
    fun `a recap paged past its cover stays on that page when the process dies in the background`() {
        openSummerRecap()
        swipeTo(mostBrewed)

        dieInBackground()
        waitFor(mostBrewed)
    }

    @Test
    fun `a season's recap leads Analytics for two weeks and stays in Recaps after`() {
        launch()
        click(text(Res.string.tab_analytics))
        waitFor(summer)

        clock.advance(10.days)
        forceStop()
        click(text(Res.string.tab_analytics))
        waitFor(text(Res.string.analytics_period_this_week))
        assertAbsent(summer)

        click(text(Res.string.recaps))
        click(text(Res.string.season_label, text(Res.string.season_summer), "2026"))
        waitFor(text(Res.string.recap_close))
    }

    @Test
    fun `a tea in Top teas opens History with only its sessions in the chosen period`() {
        launch()
        click(text(Res.string.tab_analytics))
        click(text(Res.string.analytics_period_this_week))
        click(text(Res.string.analytics_period_this_season))
        val dragonWell = hasClickAction() and hasText("Dragon Well")
        waitFor(dragonWell)
        compose.onNode(dragonWell).performScrollTo()
        click(dragonWell)

        waitFor("Sep 2")
        assertAbsent("Da Hong Pao")
        assertAbsent("Aug 20")
    }

    @Test
    fun `the chosen period survives a restart`() {
        launch()
        click(text(Res.string.tab_analytics))
        click(text(Res.string.analytics_period_this_week))
        click(text(Res.string.analytics_period_all_time))
        waitFor(text(Res.string.analytics_top_teas))

        forceStop()
        click(text(Res.string.tab_analytics))
        waitFor(text(Res.string.analytics_period_all_time))
    }

    private val summer get() = text(Res.string.recap_title_summer)

    private val mostBrewed get() = text(Res.plurals.recap_most_brewed, 6, "Dragon Well")

    private fun openSummerRecap() {
        launch()
        click(text(Res.string.tab_analytics))
        click(summer)
    }

    /** Swipes the recap's pages forward until [page] shows. */
    private fun swipeTo(page: String) {
        repeat(PAGES) {
            compose.waitForIdle()
            if (compose.onAllNodes(hasText(page)).fetchSemanticsNodes().isNotEmpty()) return
            compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeLeft() }
        }
        waitFor(page)
    }

    private companion object {
        const val PAGES = 8
    }
}
