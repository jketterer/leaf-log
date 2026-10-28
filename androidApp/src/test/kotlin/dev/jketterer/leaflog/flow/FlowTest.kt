package dev.jketterer.leaflog.flow

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.printToString
import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import dev.jketterer.leaflog.MainActivity
import dev.jketterer.leaflog.data.local.database.LeafLogDatabase
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.services.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
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
import org.json.JSONArray
import org.json.JSONObject
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
import org.robolectric.android.controller.ServiceController
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowActivity.IntentForResult
import org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock that moves only when told to, so a timer completes when the test says so. */
class TestClock(private var now: Instant = Instant.parse("2026-10-27T15:00:00Z")) : Clock {
    override fun now() = now

    fun advance(by: Duration) {
        now += by
    }

    /** Moves to [instant], back as well as forward, as when seeding sessions on past days. */
    fun set(instant: Instant) {
        now = instant
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

    private var foregroundService: ServiceController<TimerForegroundService>? = null

    @Before
    fun startApp() {
        allowNotifications(true)
        swapClock()
    }

    @After
    fun stopApp() {
        activity?.pause()?.stop()?.destroy()
        endProcess()
    }

    fun launch(savedState: Bundle? = null, intent: Intent? = null) {
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        activity = if (savedState == null) controller.setup() else controller.setup(savedState)
    }

    fun recreate() {
        activity!!.recreate()
    }

    /** Swiped from Recents: the process ends with nothing saved, and the app starts cold. */
    fun forceStop() {
        kill()
        launch()
    }

    /** Swiped from Recents and left closed: alarms and posted notifications outlive it. */
    fun kill() {
        activity!!.pause().stop().destroy()
        activity = null
        restartProcess()
    }

    /** Home pressed: the Activity stops, and the process and its timers carry on. */
    fun background() {
        activity!!.pause().stop()
    }

    /** Back to the stopped Activity from Home or Recents, the process as it was. */
    fun foreground() {
        activity!!.restart().resume()
    }

    /** Notifications allowed in system settings or not, which the user can change at any time. */
    fun allowNotifications(allowed: Boolean) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (allowed) shadowOf(app).grantPermissions(permission) else shadowOf(app).denyPermissions(permission)
    }

    /** System back, as a gesture or the button, once the last tap's screen is up. */
    fun systemBack() {
        compose.waitForIdle()
        activity!!.get().onBackPressedDispatcher.onBackPressed()
    }

    /** Whether the Activity is closing, as when back leaves the app. */
    val isFinishing: Boolean get() = activity!!.get().isFinishing

    /** The system ends the backgrounded process; returning restores the Activity's saved state. */
    fun dieInBackground() {
        val saved = Bundle()
        activity!!.pause().stop().saveInstanceState(saved).destroy()
        restartProcess()
        launch(saved.parceled())
    }

    /**
     * This build installed over the one at schema [version]: the process ends, its database is
     * replaced by one as Room made it at [version], holding the rows [seed] writes raw, and the app
     * starts again to migrate it. Call before [launch].
     */
    fun upgradeFrom(version: Int, seed: SQLiteConnection.() -> Unit) {
        // The first start's defaults are still being written to the file about to be replaced
        val startup = ProcessLifecycleOwner.get().lifecycleScope.coroutineContext.job
        waitUntil("the app has started") { startup.children.none() }
        restartProcess {
            val file = app.getDatabasePath(DATABASE)
            file.parentFile!!.mkdirs()
            listOf("", "-wal", "-shm").forEach { File(file.path + it).delete() }
            BundledSQLiteDriver().open(file.path).use { db ->
                val schema = JSONObject(File(SCHEMAS, "$version.json").readText()).getJSONObject("database")
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    val indices = entity.optJSONArray("indices") ?: JSONArray()
                    val statements = listOf(entity.getString("createSql")) +
                        List(indices.length()) { indices.getJSONObject(it).getString("createSql") }
                    statements.forEach { db.execSQL(it.replace("\${TABLE_NAME}", table)) }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                db.seed()
                db.execSQL("PRAGMA user_version = $version")
            }
        }
    }

    /** Statics survive in the JVM, so a new process is a new Koin graph on the same files. */
    private fun restartProcess(whileStopped: () -> Unit = {}) {
        endProcess()
        whileStopped()
        DeepLinkHandler.consume()
        app.onCreate()
        swapClock()
    }

    private fun endProcess() {
        foregroundService?.destroy()
        foregroundService = null
        shadowOf(app).clearStartedServices()
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

    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))

    val notificationManager: NotificationManager get() = app.getSystemService(NotificationManager::class.java)

    /** The notification posted under [id], or null if none is in the shade. */
    fun notification(id: Int): Notification? = shadowOf(notificationManager).getNotification(id)

    /** The alarm set to wake [receiver], or null if none is scheduled. */
    fun alarm(receiver: KClass<out BroadcastReceiver>): ScheduledAlarm? = alarms.scheduledAlarms
        .singleOrNull { shadowOf(it.operation).savedIntent.component?.className == receiver.java.name }

    /** Delivers [receiver]'s alarm now, as if its time had come. */
    fun fireAlarm(receiver: KClass<out BroadcastReceiver>) {
        alarms.fireAlarm(checkNotNull(alarm(receiver)) { "No alarm for ${receiver.simpleName}" })
        idle()
    }

    /** Runs what's posted to the main thread, and the timer's service as the system would. */
    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
        while (true) {
            val intent = shadowOf(app).nextStartedService ?: break
            val service = foregroundService
                ?: Robolectric.buildService(TimerForegroundService::class.java).create()
            foregroundService = service
            service.withIntent(intent).startCommand(0, 0)
        }
        // Destroying it takes its ongoing notification down
        val service = foregroundService ?: return
        if (shadowOf(service.get()).isStoppedBySelf) {
            service.destroy()
            foregroundService = null
        }
    }

    /**
     * Taps [notification] in the shade. A backgrounded app is brought back with the new intent, as
     * singleTask does; one [kill]ed starts cold from it.
     */
    fun tap(notification: Notification) {
        val intent = shadowOf(notification.contentIntent).savedIntent
        val running = activity
        if (running == null) launch(intent = intent) else running.newIntent(intent).restart().resume()
    }

    /** Waits for the app to open a system screen for a result, a file picker or save dialog, left open. */
    fun awaitPicker(): IntentForResult {
        var picker: IntentForResult? = null
        waitUntil("a picker is open") {
            picker = shadowOf(activity!!.get()).nextStartedActivityForResult
            picker != null
        }
        return picker!!
    }

    /** Fails if the app opened a system screen for a result since the last [awaitPicker]. */
    fun assertNoPicker() {
        compose.waitForIdle()
        val picker = shadowOf(activity!!.get()).peekNextStartedActivityForResult()
        check(picker == null) { "Expected no picker, but ${picker.intent} opened" }
    }

    /** The user leaves [picker] having chosen [uri], or backs out of it when null. */
    fun answer(picker: IntentForResult, uri: Uri?) {
        // Through the registry, which knows the request across a recreate; the Activity doesn't
        activity!!.get().activityResultRegistry.dispatchResult(
            picker.requestCode,
            if (uri == null) Activity.RESULT_CANCELED else Activity.RESULT_OK,
            uri?.let { Intent().setData(it) },
        )
    }

    /** Waits for [condition], naming [what] and printing the screen if it never holds. */
    fun waitUntil(what: String, condition: () -> Boolean) {
        try {
            // Run what's posted to the main thread, which only a Compose query does otherwise
            compose.waitUntil(TIMEOUT_MS) {
                idle()
                condition()
            }
        } catch (e: ComposeTimeoutException) {
            val screen = compose.onAllNodes(isRoot()).printToString(maxDepth = Int.MAX_VALUE)
            throw AssertionError("Timed out waiting until $what, on screen:\n$screen", e)
        }
    }

    fun text(res: StringResource, vararg args: Any): String = runBlocking { getString(res, *args) }

    fun text(res: PluralStringResource, count: Int, vararg args: Any): String =
        runBlocking { getPluralString(res, count, *args) }

    /** Waits for [text] as a node's text or content description, across every window. */
    fun waitFor(text: String) = waitFor(hasText(text) or hasContentDescription(text))

    fun waitFor(matcher: SemanticsMatcher) = waitUntil("${matcher.description} is on screen") {
        compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    /** Clicks the one clickable node showing [text], as its text or content description. */
    fun click(text: String) = click(hasClickAction() and (hasText(text) or hasContentDescription(text)))

    /** Clicks the one node matching [matcher], once it's on screen. */
    fun click(matcher: SemanticsMatcher) {
        waitFor(matcher)
        compose.onNode(matcher).performClick()
    }

    fun assertAbsent(text: String) {
        compose.waitForIdle()
        check(compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty()) {
            "Expected no \"$text\" on screen"
        }
    }

    /** Scrolls the screen's list until [matcher] shows. */
    fun scrollTo(matcher: SemanticsMatcher) {
        waitFor(hasScrollToNodeAction())
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(matcher)
        waitFor(matcher)
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
        const val DATABASE = "leaflog.db"
        // Tests run in androidApp/
        val SCHEMAS = File("../shared/schemas/${LeafLogDatabase::class.qualifiedName}")
    }
}
