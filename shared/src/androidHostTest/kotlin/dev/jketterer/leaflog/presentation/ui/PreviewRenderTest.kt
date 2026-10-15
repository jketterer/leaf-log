package dev.jketterer.leaflog.presentation.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/** Renders every @Preview light, dark and at 2x font scale, and fails if one crashes. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35]) // 36 needs JVM module flags on JDK 21
class PreviewRenderTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
    private val variant: Variant,
) {
    enum class Variant { Light, Dark, LargeFont }

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `renders without crashing`() {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current)
            if (variant == Variant.Dark) {
                configuration.uiMode =
                    Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
            }
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                // As in the IDE: composables skip Koin when LocalInspectionMode is set
                LocalInspectionMode provides true,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(
                    density,
                    if (variant == Variant.LargeFont) 2f else 1f
                ),
            ) {
                preview()
            }
        }
        compose.waitForIdle()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        @Suppress("unused")
        fun previews(): List<Array<Any>> = AndroidComposablePreviewScanner()
            .scanPackageTrees("dev.jketterer.leaflog")
            .includePrivatePreviews()
            .getPreviews()
            .flatMap { preview -> Variant.entries.map { arrayOf(preview, it) } }
    }
}
