package dev.jketterer.leaflog.flow

import android.Manifest
import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.printToString
import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import dev.jketterer.leaflog.MainActivity
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.begin_session
import leaf_log.shared.generated.resources.field_vessel_required
import leaf_log.shared.generated.resources.log_session
import leaf_log.shared.generated.resources.open_menu
import leaf_log.shared.generated.resources.select_tea
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock that moves only when told to, so a timer completes when the test says so. */
class TestClock(private var now: Instant = Instant.parse("2026-10-27T15:00:00Z")) : Clock {
    override fun now() = now

    fun advance(by: Duration) {
        now += by
    }
}

/**
 * Drives the real app: LeafLogApplication's Koin graph and Room file database, and MainActivity,
 * with only the clock swapped. Seed in an @Before through use cases from [koin], then [launch].
 * [forceStop] and [dieInBackground] end the process and start a new one on the same files.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi") // 36 needs JVM module flags on JDK 21
abstract class FlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    val clock = TestClock()

    val koin: Koin get() = KoinPlatform.getKoin()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private var activity: ActivityController<MainActivity>? = null

    @Before
    fun startApp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        swapClock()
    }

    @After
    fun stopApp() {
        activity?.pause()?.stop()?.destroy()
        endProcess()
    }

    fun launch(savedState: Bundle? = null) {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        activity = if (savedState == null) controller.setup() else controller.setup(savedState)
    }

    fun recreate() {
        activity!!.recreate()
    }

    /** Swiped from Recents: the process ends with nothing saved, and the app starts cold. */
    fun forceStop() {
        activity!!.pause().stop().destroy()
        restartProcess()
        launch()
    }

    /** The system ends the backgrounded process; returning restores the Activity's saved state. */
    fun dieInBackground() {
        val saved = Bundle()
        activity!!.pause().stop().saveInstanceState(saved).destroy()
        restartProcess()
        launch(saved.parceled())
    }

    /** Statics survive in the JVM, so a new process is a new Koin graph on the same files. */
    private fun restartProcess() {
        endProcess()
        DeepLinkHandler.consume()
        app.onCreate()
        swapClock()
    }

    private fun endProcess() {
        // The app scope outlives the Activity, and would keep a timer ticking into the next test
        koin.get<CoroutineScope>().cancel()
        koin.get<LeafLogDatabase>().close()
        stopKoin()
    }

    // Nothing reads the clock while the Application starts, so every holder gets this one
    private fun swapClock() = loadKoinModules(module { single<Clock> { clock } })

    /** Through a Parcel, as the system keeps it, so anything that can't be written fails here. */
    private fun Bundle.parceled(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return parcel.readBundle(MainActivity::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }
    }

    fun text(res: StringResource, vararg args: Any): String = runBlocking { getString(res, *args) }

    fun text(res: PluralStringResource, count: Int, vararg args: Any): String =
        runBlocking { getPluralString(res, count, *args) }

    /** Waits for [text] as a node's text or content description, across every window. */
    fun waitFor(text: String) = waitFor(hasText(text) or hasContentDescription(text))

    fun waitFor(matcher: SemanticsMatcher) {
        try {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            val screen = compose.onAllNodes(isRoot()).printToString()
            throw AssertionError("No ${matcher.description} on screen:\n$screen", e)
        }
    }

    /** Clicks the one clickable node showing [text], as its text or content description. */
    fun click(text: String) {
        val matcher = hasClickAction() and (hasText(text) or hasContentDescription(text))
        waitFor(matcher)
        compose.onNode(matcher).performClick()
    }

    fun assertAbsent(text: String) {
        compose.waitForIdle()
        check(compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty()) {
            "Expected no \"$text\" on screen"
        }
    }

    /** From Home to the new session's Timer, not started: Gaiwan, 5 g, 0:30. */
    fun beginSession(tea: String) {
        click(text(Res.string.open_menu))
        click(text(Res.string.log_session))
        click(text(Res.string.select_tea))
        click(tea)
        click(text(Res.string.field_vessel_required))
        click("Gaiwan")
        click("5 g")
        click("0:30")
        click(text(Res.string.begin_session))
    }

    /** Sets the screen's one RatingSelector, through the slider it presents to accessibility. */
    fun rate(stars: Int) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(stars.toFloat()) }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
