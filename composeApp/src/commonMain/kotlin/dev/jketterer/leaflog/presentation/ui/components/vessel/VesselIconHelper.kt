package dev.jketterer.leaflog.presentation.ui.components.vessel

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Coffee
import compose.icons.feathericons.Droplet

object VesselIconHelper {
    /**
     * Maps a vessel icon name to an ImageVector.
     * Currently uses FeatherIcons as placeholders until actual vessel icons are added.
     *
     * TODO: Replace with actual vessel ImageVector resources when available
     */
    fun getIconForVessel(iconName: String?): ImageVector {
        return when (iconName) {
            "gaiwan", "teapot", "yixing", "mug" -> FeatherIcons.Coffee
            "kyusu", "generic" -> FeatherIcons.Coffee
            "glass" -> FeatherIcons.Droplet
            else -> FeatherIcons.Coffee // Default fallback
        }
    }

    /**
     * Returns all available vessel icon options
     */
    fun getAllIcons(): List<VesselIconOption> {
        return listOf(
            VesselIconOption("gaiwan", "Gaiwan", FeatherIcons.Coffee),
            VesselIconOption("teapot", "Teapot", FeatherIcons.Coffee),
            VesselIconOption("kyusu", "Kyusu", FeatherIcons.Coffee),
            VesselIconOption("mug", "Mug", FeatherIcons.Coffee),
            VesselIconOption("yixing", "Yixing", FeatherIcons.Coffee),
            VesselIconOption("glass", "Glass", FeatherIcons.Droplet),
            VesselIconOption("generic", "Tea Cup", FeatherIcons.Coffee),
        )
    }
}

data class VesselIconOption(
    val iconName: String,
    val displayName: String,
    val imageVector: ImageVector
)
