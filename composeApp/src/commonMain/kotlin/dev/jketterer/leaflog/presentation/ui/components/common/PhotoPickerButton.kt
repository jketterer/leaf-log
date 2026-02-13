package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        Text(
            text = text,
            modifier = Modifier,
        )
    }
}
