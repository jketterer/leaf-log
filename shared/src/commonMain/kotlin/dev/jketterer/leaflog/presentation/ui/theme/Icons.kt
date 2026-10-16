package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Heart
import compose.icons.feathericons.Star

/** Feather's star, filled. Feather is outline-only, so its own path is filled and stroked. */
val FeatherIcons.StarFilled: ImageVector get() = starFilled

/** Feather's star with its left half filled, for a rating that ends on a half. */
val FeatherIcons.StarHalf: ImageVector get() = starHalf

/** Feather's heart, filled. */
val FeatherIcons.HeartFilled: ImageVector get() = heartFilled

private val starPath: PathBuilder.() -> Unit = {
    moveTo(12f, 2f)
    lineToRelative(3.09f, 6.26f)
    lineToRelative(6.91f, 1.01f)
    lineToRelative(-5f, 4.87f)
    lineToRelative(1.18f, 6.88f)
    lineToRelative(-6.18f, -3.25f)
    lineToRelative(-6.18f, 3.25f)
    lineToRelative(1.18f, -6.88f)
    lineToRelative(-5f, -4.87f)
    lineToRelative(6.91f, -1.01f)
    close()
}

private val starFilled by lazy { filledFeatherIcon("StarFilled", starPath) }

/** The outline stroked, and the left half of the same star filled up to the centre line. */
private val starHalf by lazy {
    featherIconBuilder("StarHalf")
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = starPath,
        )
        .path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            lineTo(8.91f, 8.26f)
            lineTo(2f, 9.27f)
            lineTo(7f, 14.14f)
            lineTo(5.82f, 21.02f)
            lineTo(12f, 17.77f)
            close()
        }
        .build()
}

private val heartFilled by lazy {
    filledFeatherIcon("HeartFilled") {
        moveTo(20.84f, 4.61f)
        arcToRelative(5.5f, 5.5f, 0f, false, false, -7.78f, 0f)
        lineTo(12f, 5.67f)
        lineToRelative(-1.06f, -1.06f)
        arcToRelative(5.5f, 5.5f, 0f, false, false, -7.78f, 7.78f)
        lineToRelative(1.06f, 1.06f)
        lineTo(12f, 21.23f)
        lineToRelative(7.78f, -7.78f)
        lineToRelative(1.06f, -1.06f)
        arcToRelative(5.5f, 5.5f, 0f, false, false, 0f, -7.78f)
        close()
    }
}

private fun featherIconBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
)

private fun filledFeatherIcon(name: String, pathBuilder: PathBuilder.() -> Unit) =
    featherIconBuilder(name).path(
        fill = SolidColor(Color.Black),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    ).build()

@Preview(showBackground = true)
@Composable
private fun FilledFeatherIconsPreview() {
    LeafLogTheme {
        Row {
            Icon(FeatherIcons.Star, contentDescription = null)
            Icon(FeatherIcons.StarHalf, contentDescription = null)
            Icon(FeatherIcons.StarFilled, contentDescription = null)
            Icon(FeatherIcons.Heart, contentDescription = null)
            Icon(FeatherIcons.HeartFilled, contentDescription = null)
        }
    }
}
