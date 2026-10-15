package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import dev.jketterer.leaflog.presentation.ui.screens.settings.teatype.colorPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeaTypeColorTest {

    private val hexes = colorPalette + listOf("#000000", "#FFFFFF", "#3E2723")

    private fun contrast(a: Color, b: Color): Float {
        val (la, lb) = a.luminance() to b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun `every colour stands out from the light surface`() {
        hexes.forEach { hex ->
            val ratio = contrast(teaTypeColor(hex, dark = false), LightColors.surface)
            assertTrue(ratio >= 3f, "$hex is $ratio:1 on light")
        }
    }

    @Test
    fun `every colour stands out from the dark surface`() {
        hexes.forEach { hex ->
            val ratio = contrast(teaTypeColor(hex, dark = true), DarkColors.surface)
            assertTrue(ratio >= 3f, "$hex is $ratio:1 on dark")
        }
    }

    @Test
    fun `a colour already in range is left alone`() {
        val toned = teaTypeColor("#795548", dark = false)
        val picked = Color(0xFF795548)
        assertTrue(abs(toned.red - picked.red) < 0.01f && abs(toned.blue - picked.blue) < 0.01f)
    }

    @Test
    fun `no two picker colours look alike on either theme`() {
        listOf(false, true).forEach { dark ->
            val toned = colorPalette.map { it to teaTypeColor(it, dark).convert(ColorSpaces.Oklab) }
            toned.forEachIndexed { i, (hexA, a) ->
                toned.drop(i + 1).forEach { (hexB, b) ->
                    val distance = sqrt((a.red - b.red).pow(2) + (a.green - b.green).pow(2) + (a.blue - b.blue).pow(2))
                    assertTrue(distance > 0.06f, "$hexA and $hexB are $distance apart, dark = $dark")
                }
            }
        }
    }

    @Test
    fun `a blank or malformed hex is unspecified`() {
        assertEquals(Color.Unspecified, teaTypeColor("", dark = false))
        assertEquals(Color.Unspecified, teaTypeColor("#12", dark = true))
    }

    @Test
    fun `text on a light colour is black and on a dark one white`() {
        assertEquals(Color.Black, contentColorOn(Color(0xFFFFEB3B)))
        assertEquals(Color.White, contentColorOn(Color(0xFF3E2723)))
    }
}
