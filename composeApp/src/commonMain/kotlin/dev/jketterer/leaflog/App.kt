import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jketterer.leaflog.presentation.ui.navigation.AppNavigation
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme

@Composable
@Preview
fun App() {
    LeafLogTheme {
        AppNavigation()
    }
}