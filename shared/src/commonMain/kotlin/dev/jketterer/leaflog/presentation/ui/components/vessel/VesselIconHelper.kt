package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

object VesselIconHelper {
    @Composable
    fun getIconForVessel(iconName: String?): Painter {
        return when (iconName) {
            "gaiwan" -> rememberVectorPainter(VesselIcons.Gaiwan)
            "teapot" -> rememberVectorPainter(VesselIcons.Teapot)
            "ic_french_press" -> rememberVectorPainter(VesselIcons.FrenchPress)
            "mug" -> rememberVectorPainter(VesselIcons.Mug)
            "kyusu" -> rememberVectorPainter(VesselIcons.Kyusu)
            "generic" -> rememberVectorPainter(VesselIcons.TeaCup)
            else -> rememberVectorPainter(VesselIcons.Mug)
        }
    }

    fun getAllIcons(): List<VesselIconOption> {
        return listOf(
            VesselIconOption("gaiwan", "Gaiwan") { rememberVectorPainter(VesselIcons.Gaiwan) },
            VesselIconOption("teapot", "Teapot") { rememberVectorPainter(VesselIcons.Teapot) },
            VesselIconOption(
                "ic_french_press",
                "French Press"
            ) { rememberVectorPainter(VesselIcons.FrenchPress) },
            VesselIconOption("kyusu", "Kyusu") { rememberVectorPainter(VesselIcons.Kyusu) },
            VesselIconOption("mug", "Mug") { rememberVectorPainter(VesselIcons.Mug) },
            VesselIconOption("generic", "Tea Cup") { rememberVectorPainter(VesselIcons.TeaCup) },
        )
    }
}

data class VesselIconOption(
    val iconName: String,
    val displayName: String,
    val painter: @Composable () -> Painter,
)
