package dev.jketterer.leaflog.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.jketterer.leaflog.domain.models.CURATED_TAG_KEYS
import dev.jketterer.leaflog.domain.models.TastingTag
import dev.jketterer.leaflog.presentation.ui.components.session.loadLabel
import kotlinx.coroutines.runBlocking
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.brew_again
import leaf_log.shared.generated.resources.brew_count
import leaf_log.shared.generated.resources.brewing_steep_n
import leaf_log.shared.generated.resources.edit_session
import leaf_log.shared.generated.resources.field_session_note
import leaf_log.shared.generated.resources.more_options
import leaf_log.shared.generated.resources.rating_out_of_five
import leaf_log.shared.generated.resources.rating_unrated
import leaf_log.shared.generated.resources.review_how_was_steep_n
import leaf_log.shared.generated.resources.review_steep_n
import leaf_log.shared.generated.resources.save
import leaf_log.shared.generated.resources.start_brewing
import leaf_log.shared.generated.resources.start_steep_n
import leaf_log.shared.generated.resources.steep_count
import leaf_log.shared.generated.resources.tab_collection
import leaf_log.shared.generated.resources.tab_history
import leaf_log.shared.generated.resources.tags_more
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/** Each released schema, seeded as that version left it, then opened by this build. */
class UpgradeFlowTest : FlowTest() {

    @Test
    fun `v0_1_1 sessions keep their steeps and ratings and every count agrees`() {
        upgradeFrom(6) {
            defaults()
            // As v0.1.1 left its stats: counted per parent, rated by steep 1 alone
            tea("dw", "Dragon Well", "totalSessions" to 9, "averageRating" to 2.0)
            method(
                "m1", "dw", "label" to "Morning", "source_session_id" to "single", "rating" to 5.0,
                "times_used" to 7, "is_active" to 1,
            )
            // Every v6 row carries the session's fields, copied when the steep was added
            val copied = arrayOf("teaQuantityGrams" to 5.0, "vesselId" to "gaiwan", "waterType" to "FILTERED")
            steep("single", "dw", ago(5.days), "rating" to 5.0, "usedConfigurationId" to "m1")
            steep("gongfu", "dw", ago(4.days), "rating" to 3.0, "averageRating" to 2.7, "usedConfigurationId" to "m1")
            steep(
                "gongfu-2", "dw", ago(4.days) + 60_000, *copied, "rating" to 1.0, "deletedAt" to ago(4.days),
                parent = "gongfu", number = 2,
            )
            steep("gongfu-3", "dw", ago(4.days) + 120_000, *copied, "rating" to 4.0, parent = "gongfu", number = 3)
            // The parent's leaf edited to 7 g after its steep was added
            steep(
                "edited", "dw", ago(3.days), "teaQuantityGrams" to 7.0, "rating" to 1.0,
                "usedConfigurationId" to "m1",
            )
            steep("edited-2", "dw", ago(3.days) + 60_000, *copied, "rating" to 3.0, parent = "edited", number = 2)
            steep("unrated", "dw", ago(2.days))
            // v6 completed the parent once steep 2 began
            steep("mid", "dw", ago(1.days), "rating" to 1.0, "usedConfigurationId" to "m1")
            steep("mid-2", "dw", ago(1.days) + 60_000, *copied, "status" to "IN_PROGRESS", parent = "mid", number = 2)
        }
        launch()

        click(text(Res.string.tab_history))
        val card = hasText("Dragon Well") and hasClickAction()
        listOf(
            card and rated("5"),
            card and rated("3.5") and hasText(text(Res.plurals.steep_count, 2, 2)),
            card and rated("2") and hasText(text(Res.plurals.steep_count, 2, 2)) and hasText("7 g"),
            card and hasContentDescription(text(Res.string.rating_unrated), substring = true),
            card and hasText(text(Res.string.brewing_steep_n, 2)),
        ).forEach { scrollTo(it) }

        // The in-progress session counts nowhere: 3.5 is the mean of 5, 3.5 and 2
        click(text(Res.string.tab_collection))
        waitFor(card and hasText(text(Res.plurals.brew_count, 4, 4)) and hasText("3.5"))
        click("Dragon Well")
        waitFor(hasText("4") and hasAnySibling(hasText("Total Sessions")))
        waitFor(hasText("3.5") and hasAnySibling(hasText("Average Rating")))
    }

    @Test
    fun `a tea deleted with sessions under v0_1_2 comes back finished and last`() {
        upgradeFrom(7) {
            defaults()
            tea("assam", "Assam", deletedAt = ago(1.days))
            tea("sencha", "Sencha")
            steep("s1", "assam", ago(2.days))
        }
        launch()

        click(text(Res.string.tab_collection))
        val assam = hasText("Assam") and hasText("Finished")
        waitFor(assam)
        waitFor("Sencha")
        assertTrue(
            "Assam should be listed after Sencha",
            compose.onNode(assam).fetchSemanticsNode().boundsInRoot.top >
                compose.onNode(hasText("Sencha")).fetchSemanticsNode().boundsInRoot.top,
        )

        click(text(Res.string.tab_history))
        waitFor("Assam")
    }

    @Test
    fun `v0_4_0 Brew Again replays every steep of an old session`() {
        upgradeFrom(8) {
            defaults()
            tea("dw", "Dragon Well")
            tea("sencha", "Sencha")
            method("m1", "dw", "label" to "Morning", "times_used" to 1)
            steep("gongfu", "dw", ago(2.days), "brewingTime" to 20_000, "usedConfigurationId" to "m1")
            steep("gongfu-2", "dw", ago(2.days) + 60_000, "brewingTime" to 25_000, parent = "gongfu", number = 2)
            steep("gongfu-3", "dw", ago(2.days) + 120_000, "brewingTime" to 40_000, parent = "gongfu", number = 3)
            steep("mid", "sencha", ago(1.days), "status" to "IN_PROGRESS")
            steep("mid-2", "sencha", ago(1.days) + 60_000, "status" to "IN_PROGRESS", parent = "mid", number = 2)
        }
        launch()

        click(text(Res.string.tab_history))
        waitFor(hasText("Sencha") and hasText(text(Res.string.brewing_steep_n, 2)))
        click(hasContentDescription(text(Res.string.more_options)) and hasAnyAncestor(hasText("Dragon Well")))
        click(text(Res.string.brew_again))
        click(text(Res.string.start_brewing))
        waitFor("Sencha")
        click("Complete & Start New")

        waitFor("0:20")
        click(text(Res.string.start_brewing))
        clock.advance(20.seconds)
        waitFor(text(Res.string.review_how_was_steep_n, 1))
        click(text(Res.string.tags_more))
        runBlocking { CURATED_TAG_KEYS.map { TastingTag(it, it, null, false, 0).loadLabel() } }.forEach(::waitFor)
        // The sheet's own window takes back, so close it as TalkBack does
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
        click(text(Res.string.start_steep_n, 2))
        waitFor("0:25")
        click(text(Res.string.start_brewing))
        clock.advance(25.seconds)
        click(text(Res.string.start_steep_n, 3))
        waitFor("0:40")
    }

    @Test
    fun `a v0_5_0 review note becomes the session's and steep notes stay their own`() {
        upgradeFrom(11) {
            defaults()
            tea("dw", "Dragon Well")
            // v0.5.0 kept the review note on steep 1
            steep("s", "dw", ago(1.days), "notes" to "Lovely and sweet", "rating" to 4.0)
            steep(
                "s-2", "dw", ago(1.days) + 60_000, "notes" to "Bitter finish", "rating" to 4.0,
                parent = "s", number = 2,
            )
        }
        launch()

        click(text(Res.string.tab_history))
        click(hasText("Dragon Well") and hasText("Lovely and sweet") and hasClickAction())
        val steep = { n: Int -> hasText(text(Res.string.review_steep_n, n)) and hasClickAction() }
        waitFor(steep(2) and hasText("Bitter finish"))
        compose.onNode(steep(1)).assert(!hasText("Lovely and sweet"))
        waitFor("Lovely and sweet")

        click(text(Res.string.more_options))
        click(text(Res.string.edit_session))
        val note = hasSetTextAction() and hasText(text(Res.string.field_session_note))
        scrollTo(note and hasText("Lovely and sweet"))
        compose.onNode(note).performTextReplacement("Lovely, sweeter cooled")
        click(text(Res.string.save))
        waitFor(steep(1))
        waitFor("Lovely, sweeter cooled")
        compose.onNode(steep(1)).assert(!hasText("Lovely, sweeter cooled"))
    }

    private fun rated(rating: String) =
        hasContentDescription(text(Res.string.rating_out_of_five, rating), substring = true)

    private fun ago(duration: Duration) = (clock.now() - duration).toEpochMilliseconds()
}

/** One row, from column-value pairs; strings are quoted, anything else is written as is. */
private fun SQLiteConnection.insert(table: String, vararg values: Pair<String, Any?>) = execSQL(
    "INSERT INTO $table (${values.joinToString { it.first }}) VALUES (" +
        values.joinToString { (_, v) -> if (v is String) "'${v.replace("'", "''")}'" else "$v" } + ")",
)

/** The Green type and the Gaiwan, as every version seeded them. */
private fun SQLiteConnection.defaults() {
    insert(
        "tea_type", "id" to "green", "name" to "Green", "colorHex" to "#4CAF50",
        "isSystemDefault" to 1, "displayOrder" to 0, "createdAt" to 0, "updatedAt" to 0,
    )
    insert(
        "brewing_vessel", "id" to "gaiwan", "name" to "Gaiwan", "isSystemDefault" to 1, "isArchived" to 0,
        "displayOrder" to 0, "createdAt" to 0, "updatedAt" to 0, "syncStatus" to "LOCAL_ONLY",
    )
}

private fun SQLiteConnection.method(id: String, teaId: String, vararg extra: Pair<String, Any?>) = insert(
    "brewing_configurations", "id" to id, "tea_id" to teaId, "vessel_id" to "gaiwan", "tea_quantity_grams" to 5.0,
    "water_quantity_ml" to 100.0, "temperature_celsius" to 95.0, "brewing_time" to 20_000, "water_type" to "FILTERED",
    "is_pinned" to 0, "pinned_sort_order" to 0, "created_at" to 0, "updated_at" to 0, *extra,
)

private fun SQLiteConnection.tea(
    id: String,
    name: String,
    vararg extra: Pair<String, Any?>,
    deletedAt: Long? = null,
) = insert(
    "tea", "id" to id, "name" to name, "teaTypeId" to "green", "photos" to "[]", "isFavorite" to 0,
    "totalSessions" to 0, "createdAt" to 0, "updatedAt" to 0, "deletedAt" to deletedAt,
    "syncStatus" to "LOCAL_ONLY", *extra,
)

/**
 * A completed 0:30 steep at 95° with 100 mL. Without a [parent] it is steep 1, holding the
 * session's 5 g in the Gaiwan with filtered water. [extra] adds or overrides columns.
 */
private fun SQLiteConnection.steep(
    id: String,
    teaId: String,
    timestamp: Long,
    vararg extra: Pair<String, Any?>,
    parent: String? = null,
    number: Int = 1,
) {
    val row = linkedMapOf<String, Any?>(
        "id" to id, "teaId" to teaId, "parentSessionId" to parent, "steepNumber" to number,
        "status" to "COMPLETED", "timestamp" to timestamp, "brewingTime" to 30_000,
        "temperatureCelsius" to 95.0, "waterQuantityMl" to 100.0, "photos" to "[]",
        "syncStatus" to "LOCAL_ONLY", "createdAt" to timestamp, "updatedAt" to timestamp,
    )
    if (parent == null) row += listOf("teaQuantityGrams" to 5.0, "vesselId" to "gaiwan", "waterType" to "FILTERED")
    row += extra
    insert("tea_session", *row.toList().toTypedArray())
}
