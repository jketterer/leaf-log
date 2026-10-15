package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.data.local.ImageStorage
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PhotoGrid(
    photos: List<String>,
    onAddPhoto: (ByteArray) -> Unit,
    onRemovePhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Null in a preview, where no Koin application is running and resolving would throw.
    imageStorage: ImageStorage? = if (LocalInspectionMode.current) null else koinInject(),
) {
    val scope = rememberCoroutineScope()
    var expandedPhotoIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photos.isNotEmpty() && imageStorage != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                itemsIndexed(photos, key = { _, path -> path }) { index, photoPath ->
                    Box {
                        AsyncImage(
                            model = imageStorage.resolveImagePath(photoPath),
                            contentDescription = "Session photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { expandedPhotoIndex = index },
                            contentScale = ContentScale.Crop,
                        )
                        if (enabled) {
                            IconButton(
                                onClick = { onRemovePhoto(photoPath) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.X,
                                    contentDescription = "Remove photo",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (enabled && imageStorage != null) {
            PhotoPickerButton(
                onPhotoPicked = { file ->
                    scope.launch { onAddPhoto(file.readBytes()) }
                },
                text = if (photos.isEmpty()) "Add Photo" else "Add Another Photo",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (expandedPhotoIndex >= 0 && imageStorage != null) {
        FullscreenImageViewer(
            photos = photos.map { imageStorage.resolveImagePath(it) },
            initialIndex = expandedPhotoIndex,
            onDismiss = { expandedPhotoIndex = -1 },
        )
    }
}
