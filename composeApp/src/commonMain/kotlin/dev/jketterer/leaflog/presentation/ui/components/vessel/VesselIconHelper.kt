package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import leaflog.composeapp.generated.resources.Res
import leaflog.composeapp.generated.resources.ic_french_press
import leaflog.composeapp.generated.resources.ic_teapot
import org.jetbrains.compose.resources.painterResource

object VesselIconHelper {
    @Composable
    fun getIconForVessel(iconName: String?): Painter {
        return when (iconName) {
            "gaiwan" -> rememberVectorPainter(VesselIcons.Gaiwan)
            "teapot" -> painterResource(Res.drawable.ic_teapot)
            "ic_french_press" -> painterResource(Res.drawable.ic_french_press)
            "mug" -> rememberVectorPainter(VesselIcons.Mug)
            "kyusu" -> rememberVectorPainter(VesselIcons.Kyusu)
            "generic" -> rememberVectorPainter(VesselIcons.TeaCup)
            else -> rememberVectorPainter(VesselIcons.Mug)
        }
    }

    fun getAllIcons(): List<VesselIconOption> {
        return listOf(
            VesselIconOption("gaiwan", "Gaiwan") { rememberVectorPainter(VesselIcons.Gaiwan) },
            VesselIconOption("teapot", "Teapot") { painterResource(Res.drawable.ic_teapot) },
            VesselIconOption(
                "ic_french_press",
                "French Press"
            ) { painterResource(Res.drawable.ic_french_press) },
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
