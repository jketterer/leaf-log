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
}

private val iconStroke = SolidColor(Color(0xFF000000))

private fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) {
    path(
        stroke = iconStroke,
        strokeLineWidth = 2f,
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
 * Gaiwan (Chinese lidded bowl): three-part design — a wide saucer at the bottom,
 * a trapezoidal bowl body (wider at the top), and an arched lid with a knob.
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
            // Wide saucer arc at the bottom
            moveTo(3f, 21f)
            quadTo(12f, 23f, 21f, 21f)
        }
        strokePath {
            // Bowl body (trapezoid: 14-wide at top, 10-wide at bottom)
            moveTo(5f, 11f)
            lineTo(19f, 11f)
            lineTo(17f, 20f)
            lineTo(7f, 20f)
            close() // left wall back to (5,11)
        }
        strokePath {
            // Lid arching from rim to rim, with a knob at the peak
            moveTo(5f, 11f)
            quadTo(12f, 6f, 19f, 11f)
            moveTo(12f, 6f)
            lineTo(12f, 4f)
        }
    }.build()

/**
 * Kyusu (Japanese teapot): rounded rectangular body, short horizontal front spout,
 * and a distinctive over-the-top arc handle (yokode style).
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
            // Rounded rectangular body
            moveTo(6f, 8f)
            lineTo(15f, 8f)
            quadTo(16f, 8f, 16f, 9f)
            lineTo(16f, 18f)
            quadTo(16f, 19f, 15f, 19f)
            lineTo(6f, 19f)
            quadTo(5f, 19f, 5f, 18f)
            lineTo(5f, 9f)
            quadTo(5f, 8f, 6f, 8f)
            close()
        }
        strokePath {
            // Short, slightly angled horizontal spout on the right
            moveTo(16f, 12f)
            lineTo(21f, 11f)
        }
        strokePath {
            // Over-the-top arc handle (characteristic kyusu feature)
            moveTo(7f, 8f)
            quadTo(7f, 4f, 11f, 4f)
            quadTo(15f, 4f, 15f, 8f)
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
