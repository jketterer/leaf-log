package dev.jketterer.leaflog.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.figtree_bold
import leaf_log.shared.generated.resources.figtree_medium
import leaf_log.shared.generated.resources.figtree_regular
import leaf_log.shared.generated.resources.figtree_semibold
import org.jetbrains.compose.resources.Font

/** Figtree for display, headline and title styles; body and labels stay system, for density. */
@Composable
internal fun leafLogTypography(): Typography {
    val figtree = FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_medium, FontWeight.Medium),
        Font(Res.font.figtree_semibold, FontWeight.SemiBold),
        Font(Res.font.figtree_bold, FontWeight.Bold),
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = figtree),
        displayMedium = base.displayMedium.copy(fontFamily = figtree),
        displaySmall = base.displaySmall.copy(fontFamily = figtree),
        headlineLarge = base.headlineLarge.copy(fontFamily = figtree),
        headlineMedium = base.headlineMedium.copy(fontFamily = figtree),
        headlineSmall = base.headlineSmall.copy(fontFamily = figtree),
        titleLarge = base.titleLarge.copy(fontFamily = figtree),
        titleMedium = base.titleMedium.copy(fontFamily = figtree),
        titleSmall = base.titleSmall.copy(fontFamily = figtree),
    )
}
