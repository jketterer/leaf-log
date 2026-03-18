package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType

@Composable
fun PhotoPickerButton(
    onPhotoPicked: (PlatformFile) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Add Photo",
) {
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Image,
    ) { file ->
        file?.let { onPhotoPicked(it) }
    }

    Button(
        onClick = { launcher.launch() },
        modifier = modifier,
    ) {
        Icon(
            imageVector = FeatherIcons.Camera,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = text,
            modifier = Modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoPickerButtonPreview() {
    LeafLogTheme {
        PhotoPickerButton(onPhotoPicked = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoPickerButtonCustomTextPreview() {
    LeafLogTheme {
        PhotoPickerButton(onPhotoPicked = {}, text = "Change Photo")
    }
}
