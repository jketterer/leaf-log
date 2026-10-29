package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object VesselIcons {
    val Mug: ImageVector by lazy { buildMugIcon() }
    val Gaiwan: ImageVector by lazy { buildGaiwanIcon() }
    val Kyusu: ImageVector by lazy { buildKyusuIcon() }
    val TeaCup: ImageVector by lazy { buildTeaCupIcon() }
    val Teapot: ImageVector by lazy { buildTeapotIcon() }
    val FrenchPress: ImageVector by lazy { buildFrenchPressIcon() }
}

private val iconStroke = SolidColor(Color(0xFF000000))

private fun ImageVector.Builder.strokePath(
    width: Float = 2f,
    block: PathBuilder.() -> Unit,
) {
    path(
        stroke = iconStroke,
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

/**
 * Classic mug: rectangular body with rounded bottom corners and a D-shaped handle.
 */
private fun buildMugIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.Mug",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Body with rounded bottom corners
            moveTo(4f, 6f)
            lineTo(16f, 6f)
            lineTo(16f, 19f)
            quadTo(16f, 21f, 14f, 21f)
            lineTo(6f, 21f)
            quadTo(4f, 21f, 4f, 19f)
            close() // draws left wall back to (4,6)
        }
        strokePath {
            // D-shaped handle on the right
            moveTo(16f, 10f)
            quadTo(21f, 10f, 21f, 14f)
            quadTo(21f, 18f, 16f, 18f)
        }
    }.build()

/**
 * Gaiwan (Chinese lidded bowl): a domed lid with a knob sitting inside a flared lip, a tulip-shaped
 * bowl narrowing to its base, and a flat saucer.
 */
private fun buildGaiwanIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.Gaiwan",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Knob
            moveTo(12f, 3.5f)
            lineTo(12f, 5f)
        }
        strokePath {
            // Domed lid, narrower than the lip it sits in
            moveTo(6.5f, 9f)
            curveTo(7f, 6.8f, 9.2f, 5.5f, 12f, 5.5f)
            curveTo(14.8f, 5.5f, 17f, 6.8f, 17.5f, 9f)
        }
        strokePath {
            // Bowl: flared lip curving in to a narrow base
            moveTo(3.5f, 9f)
            lineTo(20.5f, 9f)
            quadTo(18f, 10f, 17.5f, 13f)
            quadTo(16.5f, 18f, 13.5f, 18f)
            lineTo(10.5f, 18f)
            quadTo(7.5f, 18f, 6.5f, 13f)
            quadTo(6f, 10f, 3.5f, 9f)
            close()
        }
        strokePath {
            // Saucer
            moveTo(4f, 21f)
            lineTo(20f, 21f)
        }
    }.build()

/**
 * Kyusu (Japanese teapot): a squat round body with a domed lid, a short rounded spout, and the
 * straight side handle (yokode) angled toward the viewer that sets it apart from a western teapot.
 */
private fun buildKyusuIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.Kyusu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Knob
            moveTo(11f, 6.5f)
            lineTo(11f, 8f)
        }
        strokePath {
            // Domed lid
            moveTo(7.5f, 11.5f)
            curveTo(7.5f, 9.5f, 9f, 8.5f, 11f, 8.5f)
            curveTo(13f, 8.5f, 14.5f, 9.5f, 14.5f, 11.5f)
        }
        strokePath {
            // Squat body, flat across the top
            moveTo(5f, 11.5f)
            lineTo(17f, 11.5f)
            curveTo(17f, 17.5f, 14.5f, 20f, 11f, 20f)
            curveTo(7.5f, 20f, 5f, 17.5f, 5f, 11.5f)
            close()
        }
        strokePath {
            // Short rounded spout on the right
            moveTo(17f, 13.5f)
            quadTo(19.5f, 12.5f, 20.5f, 11f)
            quadTo(21.5f, 12f, 20.5f, 14.5f)
            quadTo(19.5f, 16.3f, 16.2f, 17f)
        }
        strokePath(width = 3.5f) {
            // Side handle, a solid rod angled toward the viewer
            moveTo(6f, 16.5f)
            lineTo(2.3f, 19.3f)
        }
    }.build()

/**
 * Generic tea cup: wide-rimmed bowl shape (tapered, no handle) resting on a saucer.
 */
private fun buildTeaCupIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.TeaCup",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Tapered cup body (no handle)
            moveTo(5f, 7f)
            lineTo(19f, 7f)
            lineTo(16f, 20f)
            lineTo(8f, 20f)
            close() // left wall back to (5,7)
        }
        strokePath {
            // Saucer arc underneath
            moveTo(4f, 21f)
            quadTo(12f, 23f, 20f, 21f)
        }
    }.build()

/**
 * Western teapot: a round dome body with a lid and knob, a loop handle on the left, and a spout
 * curving up on the right.
 */
private fun buildTeapotIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.Teapot",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Knob
            moveTo(12f, 5f)
            lineTo(12f, 6.5f)
        }
        strokePath {
            // Lid
            moveTo(9f, 8.5f)
            quadTo(12f, 6.5f, 15f, 8.5f)
        }
        strokePath {
            // Dome body on a flat base
            moveTo(6.5f, 19.5f)
            curveTo(5.5f, 17f, 5.5f, 12f, 8f, 10f)
            curveTo(9.3f, 9f, 10.5f, 8.5f, 12f, 8.5f)
            curveTo(13.5f, 8.5f, 14.7f, 9f, 16f, 10f)
            curveTo(18.5f, 12f, 18.5f, 17f, 17.5f, 19.5f)
            close()
        }
        strokePath {
            // Loop handle on the left, held clear of the body
            moveTo(6.3f, 11.5f)
            lineTo(4f, 11.5f)
            quadTo(1.2f, 11.5f, 1.2f, 14f)
            lineTo(1.2f, 15f)
            quadTo(1.2f, 17.5f, 4f, 17.5f)
            lineTo(5.6f, 17.5f)
        }
        strokePath {
            // Spout curving up on the right
            moveTo(18.3f, 16f)
            quadTo(21f, 15f, 21.5f, 10.5f)
        }
    }.build()

/**
 * French press: a straight carafe with a lid, a plunger rod and knob above it, the filter partway
 * down, a handle on the right, and a base plate under it.
 */
private fun buildFrenchPressIcon(): ImageVector =
    ImageVector.Builder(
        name = "VesselIcons.FrenchPress",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokePath {
            // Plunger knob and rod
            moveTo(8.5f, 1.5f)
            lineTo(11.5f, 1.5f)
            moveTo(10f, 1.5f)
            lineTo(10f, 5.5f)
        }
        strokePath {
            // Lid
            moveTo(4.5f, 5.5f)
            lineTo(15.5f, 5.5f)
        }
        strokePath {
            // Carafe walls, standing on the base
            moveTo(5.5f, 5.5f)
            lineTo(5.5f, 19.5f)
            moveTo(14.5f, 19.5f)
            lineTo(14.5f, 5.5f)
        }
        strokePath {
            // Filter
            moveTo(5.5f, 11.5f)
            lineTo(14.5f, 11.5f)
        }
        strokePath {
            // Handle on the right
            moveTo(14.5f, 8f)
            lineTo(17.5f, 8f)
            quadTo(19.5f, 8f, 19.5f, 10f)
            lineTo(19.5f, 14f)
            quadTo(19.5f, 16f, 17.5f, 16f)
            lineTo(14.5f, 16f)
        }
        strokePath {
            // Base plate
            moveTo(4.5f, 19.5f)
            lineTo(15.5f, 19.5f)
            lineTo(15.5f, 21f)
            lineTo(4.5f, 21f)
            close()
        }
    }.build()
