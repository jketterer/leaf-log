package dev.jketterer.leaflog.flow

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.backup_exported
import leaf_log.shared.generated.resources.brew_count
import leaf_log.shared.generated.resources.couldnt_open_backup
import leaf_log.shared.generated.resources.edit
import leaf_log.shared.generated.resources.failure_not_a_backup
import leaf_log.shared.generated.resources.field_steep_increment
import leaf_log.shared.generated.resources.method_usage
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.plan_steep_label
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.rinsed
import leaf_log.shared.generated.resources.settings_title
import leaf_log.shared.generated.resources.tab_collection
import leaf_log.shared.generated.resources.tab_history
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/** Settings' export and import, through the system's save dialog and file picker. */
class BackupFlowTest : FlowTest() {

    @get:Rule
    val files = TemporaryFolder()

    @Test
    fun `an export is saved under the time it was made and Settings shows it from then on`() {
        launch()
        click(text(Res.string.settings_title))
        waitFor("Last backup: never")

        val first = export("Back up now")
        waitFor(text(Res.string.backup_exported))
        assertNotNull(ZipFile(first).getEntry("data.json"))
        waitFor("Last backup: today")
        // It covers the button
        waitUntil("the snackbar has gone") {
            compose.onAllNodes(hasText(text(Res.string.backup_exported))).fetchSemanticsNodes().isEmpty()
        }

        clock.advance(5.seconds)
        val second = export("Export")
        assertNotNull(ZipFile(second).getEntry("data.json"))

        forceStop()
        click(text(Res.string.settings_title))
        waitFor("Last backup: today")
        waitFor(hasText("Export") and hasClickAction())
    }

    @Test
    fun `backing out of the save dialog leaves the last backup as it was`() {
        launch()
        click(text(Res.string.settings_title))
        tapSetting("Back up now")
        answer(awaitPicker(), null)

        compose.onNode(hasText("Back up now") and hasClickAction()).assertIsEnabled()
        waitFor("Last backup: never")
        assertAbsent(text(Res.string.backup_exported))
    }

    @Test
    fun `the save dialog opens once across a recreate and still saves`() {
        launch()
        click(text(Res.string.settings_title))
        tapSetting("Back up now")
        val picker = awaitPicker()
        recreate()
        assertNoPicker()

        val file = files.newFile(picker.intent.getStringExtra(Intent.EXTRA_TITLE)!!)
        answer(picker, Uri.fromFile(file))
        waitFor(text(Res.string.backup_exported))
        assertNotNull(ZipFile(file).getEntry("data.json"))
    }

    @Test
    fun `a v0_1_1 backup brings its sessions and photos and every count agrees with them`() {
        launch()
        click(text(Res.string.settings_title))
        waitFor(hasText("0 KB") and hasAnySibling(hasText("Photos")))

        import(backup(V0_1_1, "images/tea_photos/0.jpg" to Random.nextBytes(5_000)))
        // Measured again without leaving Settings
        waitFor(hasText("5 KB") and hasAnySibling(hasText("Photos")))

        // 4.5 is the mean of 5 and steeps 3 and 5's 4
        systemBack()
        click(text(Res.string.tab_collection))
        waitFor(hasText("Dragon Well") and hasText(text(Res.plurals.brew_count, 2, 2)) and hasText("4.5"))
        click("Dragon Well")
        waitFor(hasText("2") and hasAnySibling(hasText("Total Sessions")))
        waitFor(hasText("4.5") and hasAnySibling(hasText("Average Rating")))
        scrollTo(hasText("Morning"))
        // How long ago is left off
        waitFor(hasText(text(Res.plurals.method_usage, 2, 2, "").trimEnd(), substring = true))
    }

    @Test
    fun `a v0_4_0 backup imports with no plans or rinses`() {
        launch()
        click(text(Res.string.settings_title))
        import(backup(V0_4_0))
        systemBack()

        click(text(Res.string.tab_history))
        click(hasText("Dragon Well") and hasClickAction())
        waitFor(hasText(text(Res.string.review_steep_n, 2)) and hasClickAction())
        assertAbsent(text(Res.string.rinsed))
        systemBack()

        click(text(Res.string.tab_collection))
        click("Dragon Well")
        scrollTo(hasText("Morning"))
        click(hasContentDescription(text(Res.string.more_options)) and hasAnySibling(hasText("Morning")))
        click(text(Res.string.edit))
        waitFor(text(Res.string.field_steep_increment))
        assertAbsent(text(Res.string.plan_steep_label, 2))
    }

    @Test
    fun `a v0_5_0 backup's steep 1 note arrives as the session's`() {
        launch()
        click(text(Res.string.settings_title))
        import(backup(V0_5_0))
        systemBack()

        click(text(Res.string.tab_history))
        click(hasText("Dragon Well") and hasText("Lovely and sweet") and hasClickAction())
        val steep = { n: Int -> hasText(text(Res.string.review_steep_n, n)) and hasClickAction() }
        waitFor(steep(2) and hasText("Bitter finish"))
        compose.onNode(steep(1)).assert(!hasText("Lovely and sweet"))
        waitFor("Lovely and sweet")
    }

    @Test
    fun `backing out of the file picker changes nothing and says nothing`() {
        launch()
        click(text(Res.string.settings_title))
        tapSetting("Import")
        answer(awaitPicker(), null)

        compose.onNode(hasText("Import") and hasClickAction()).assertIsEnabled()
        assertAbsent(text(Res.string.couldnt_open_backup))
        assertNoPicker()
    }

    @Test
    fun `a file that isn't a backup is refused with the reason`() {
        launch()
        click(text(Res.string.settings_title))
        tapSetting("Import")
        val notes = files.newFile("notes.txt").apply { writeText("Dragon Well, 80°") }
        answer(awaitPicker(), Uri.fromFile(notes))

        waitFor(text(Res.string.failure_not_a_backup))
        compose.onNode(hasText("Import") and hasClickAction()).assertIsEnabled()
        assertAbsent("Import this backup?")
    }

    @Test
    fun `the file picker opens once across a recreate and still previews`() {
        launch()
        click(text(Res.string.settings_title))
        tapSetting("Import")
        val picker = awaitPicker()
        recreate()
        assertNoPicker()

        answer(picker, backup(V0_5_0))
        waitFor("Import this backup?")
    }

    /** Saves an export through the dialog [button] opens, under the name it suggests, and returns it. */
    private fun export(button: String): File {
        tapSetting(button)
        val picker = awaitPicker()
        val made = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())
            .format(java.time.Instant.ofEpochMilli(clock.now().toEpochMilliseconds()))
        val name = picker.intent.getStringExtra(Intent.EXTRA_TITLE)
        assertEquals("leaf-log-export-$made.zip", name)
        val file = files.newFile(name)
        answer(picker, Uri.fromFile(file))
        waitUntil("$name is written") { file.length() > 0 }
        return file
    }

    /** Picks [backup] from Settings and imports all of it. */
    private fun import(backup: Uri) {
        tapSetting("Import")
        answer(awaitPicker(), backup)
        waitFor("Import this backup?")
        click(hasText("Import") and hasClickAction() and hasAnyAncestor(isDialog()))
        waitFor("Import Complete")
        click("OK")
    }

    /** Settings' rows run past the screen, so its buttons are scrolled to first. */
    private fun tapSetting(label: String) {
        val button = hasText(label) and hasClickAction()
        waitFor(button)
        compose.onNode(button).performScrollTo().performClick()
    }

    /** A backup file as a release wrote it: [json] as `data.json`, and [photos] by archive path. */
    private fun backup(json: String, vararg photos: Pair<String, ByteArray>): Uri {
        val file = files.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            (listOf("data.json" to json.toByteArray()) + photos).forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return Uri.fromFile(file)
    }
}

/**
 * v0.1.1's export: the Room entities as they were, defaults left out. Its stats are stale, and steep
 * 2 carries the session's fields as v0.1.1 copied them.
 */
private val V0_1_1 = """
{
  "exportedAt": 1792400000000,
  "teaTypes": [{
    "id": "green", "name": "Green", "defaultTemperatureCelsius": 80.0, "colorHex": "#4CAF50",
    "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "brewingVessels": [{
    "id": "gaiwan", "name": "Gaiwan", "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "teas": [{
    "id": "dw", "name": "Dragon Well", "teaTypeId": "green", "photos": ["images/tea_photos/0.jpg"],
    "totalSessions": 9, "averageRating": 2.0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "teaSessions": [
    {
      "id": "single", "teaId": "dw", "parentSessionId": null, "steepNumber": 1, "status": "COMPLETED",
      "teaQuantityGrams": 5.0, "vesselId": "gaiwan", "waterType": "FILTERED", "location": null,
      "rating": 5.0, "averageRating": 5.0, "usedConfigurationId": "m1",
      "timestamp": "2026-10-10T09:00:00Z", "brewingTime": "PT20S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "notes": null, "photos": [], "userId": null, "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-10-10T09:00:00Z", "updatedAt": "2026-10-10T09:00:00Z", "deletedAt": null
    },
    {
      "id": "gongfu", "teaId": "dw", "parentSessionId": null, "steepNumber": 1, "status": "COMPLETED",
      "teaQuantityGrams": 5.0, "vesselId": "gaiwan", "waterType": "FILTERED", "location": null,
      "rating": 3.0, "averageRating": 3.0, "usedConfigurationId": "m1",
      "timestamp": "2026-10-11T09:00:00Z", "brewingTime": "PT20S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "notes": null, "photos": [], "userId": null, "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-10-11T09:00:00Z", "updatedAt": "2026-10-11T09:00:00Z", "deletedAt": null
    },
    {
      "id": "gongfu-2", "teaId": "dw", "parentSessionId": "gongfu", "steepNumber": 2, "status": "COMPLETED",
      "teaQuantityGrams": 5.0, "vesselId": "gaiwan", "waterType": "FILTERED", "location": null,
      "rating": 5.0, "averageRating": null, "usedConfigurationId": "m1",
      "timestamp": "2026-10-11T09:01:00Z", "brewingTime": "PT25S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "notes": null, "photos": [], "userId": null, "syncStatus": "LOCAL_ONLY",
      "createdAt": "2026-10-11T09:01:00Z", "updatedAt": "2026-10-11T09:01:00Z", "deletedAt": null
    }
  ],
  "brewingConfigurations": [{
    "id": "m1", "teaId": "dw", "vesselId": "gaiwan", "teaQuantityGrams": 5.0, "waterQuantityMl": 100.0,
    "temperatureCelsius": 80.0, "brewingTime": "PT20S", "waterType": "FILTERED",
    "sourceSessionId": "single", "rating": 5.0, "timesUsed": 7, "lastUsedAt": null, "label": "Morning",
    "isActive": true, "createdAt": "2026-10-10T09:05:00Z", "updatedAt": "2026-10-10T09:05:00Z"
  }]
}
"""

/** v0.4.0's export, version 2: from before plans, rinses and tags. */
private val V0_4_0 = """
{
  "version": 2,
  "exportedAt": "2026-10-20T10:00:00Z",
  "teaTypes": [{
    "id": "green", "name": "Green", "defaultTemperatureCelsius": 80.0, "colorHex": "#4CAF50",
    "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "vessels": [{
    "id": "gaiwan", "name": "Gaiwan", "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "teas": [{
    "id": "dw", "name": "Dragon Well", "teaTypeId": "green",
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "steeps": [
    {
      "id": "gongfu", "teaId": "dw", "steepNumber": 1, "status": "COMPLETED", "teaQuantityGrams": 5.0,
      "vesselId": "gaiwan", "waterType": "FILTERED", "usedConfigurationId": "m1", "rating": 4.0,
      "timestamp": "2026-10-11T09:00:00Z", "brewingTime": "PT20S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "createdAt": "2026-10-11T09:00:00Z", "updatedAt": "2026-10-11T09:00:00Z"
    },
    {
      "id": "gongfu-2", "teaId": "dw", "parentSessionId": "gongfu", "steepNumber": 2, "status": "COMPLETED",
      "timestamp": "2026-10-11T09:01:00Z", "brewingTime": "PT25S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "createdAt": "2026-10-11T09:01:00Z", "updatedAt": "2026-10-11T09:01:00Z"
    }
  ],
  "methods": [{
    "id": "m1", "teaId": "dw", "vesselId": "gaiwan", "teaQuantityGrams": 5.0, "waterQuantityMl": 100.0,
    "temperatureCelsius": 80.0, "brewingTime": "PT20S", "waterType": "FILTERED", "label": "Morning",
    "createdAt": "2026-10-10T09:05:00Z", "updatedAt": "2026-10-10T09:05:00Z"
  }]
}
"""

/** v0.5.0's export, version 3: the review note on steep 1, as each steep's own note. */
private val V0_5_0 = """
{
  "version": 3,
  "exportedAt": "2026-10-25T10:00:00Z",
  "teaTypes": [{
    "id": "green", "name": "Green", "defaultTemperatureCelsius": 80.0, "colorHex": "#4CAF50",
    "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "vessels": [{
    "id": "gaiwan", "name": "Gaiwan", "isSystemDefault": true, "displayOrder": 0,
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "teas": [{
    "id": "dw", "name": "Dragon Well", "teaTypeId": "green",
    "createdAt": "2026-10-01T10:00:00Z", "updatedAt": "2026-10-01T10:00:00Z"
  }],
  "steeps": [
    {
      "id": "s", "teaId": "dw", "steepNumber": 1, "status": "COMPLETED", "teaQuantityGrams": 5.0,
      "vesselId": "gaiwan", "waterType": "FILTERED", "rating": 4.0, "notes": "Lovely and sweet",
      "timestamp": "2026-10-24T09:00:00Z", "brewingTime": "PT20S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "createdAt": "2026-10-24T09:00:00Z", "updatedAt": "2026-10-24T09:00:00Z"
    },
    {
      "id": "s-2", "teaId": "dw", "parentSessionId": "s", "steepNumber": 2, "status": "COMPLETED",
      "rating": 4.0, "notes": "Bitter finish",
      "timestamp": "2026-10-24T09:01:00Z", "brewingTime": "PT25S", "temperatureCelsius": 80.0,
      "waterQuantityMl": 100.0, "createdAt": "2026-10-24T09:01:00Z", "updatedAt": "2026-10-24T09:01:00Z"
    }
  ],
  "methods": []
}
"""
