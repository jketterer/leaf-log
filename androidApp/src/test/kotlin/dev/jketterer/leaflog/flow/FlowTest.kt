package dev.jketterer.leaflog.flow

import android.Manifest
import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dev.jketterer.leaflog.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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
import org.robolectric.RobolectricTestRunner
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
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi") // 36 needs JVM module flags on JDK 21
abstract class FlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    val clock = TestClock()

    val koin: Koin get() = KoinPlatform.getKoin()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun startApp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // Nothing reads the clock while the Application starts, so every holder gets this one
        loadKoinModules(module { single<Clock> { clock } })
    }

    @After
    fun stopApp() {
        scenario?.close()
        // The app scope outlives the Activity, and would keep a timer ticking into the next test
        koin.get<CoroutineScope>().cancel()
        stopKoin()
    }

    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    fun recreate() {
        scenario!!.recreate()
    }

    fun text(res: StringResource, vararg args: Any): String = runBlocking { getString(res, *args) }

    fun text(res: PluralStringResource, count: Int, vararg args: Any): String =
        runBlocking { getPluralString(res, count, *args) }

    /** Waits for [text] as a node's text or content description, across every window. */
    fun waitFor(text: String) = waitFor(hasText(text) or hasContentDescription(text))

    fun waitFor(matcher: SemanticsMatcher) = compose.waitUntil(TIMEOUT_MS) {
        compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
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

    /** Sets the screen's one RatingSelector, through the slider it presents to accessibility. */
    fun rate(stars: Int) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(stars.toFloat()) }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
