package dev.jketterer.leaflog.presentation.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import dev.jketterer.leaflog.presentation.ui.theme.LeafLogTheme
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

@Composable
fun PhotoGrid(
    photos: List<String>,
    onAddPhoto: (ByteArray) -> Unit,
    onRemovePhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var expandedPhotoIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                itemsIndexed(photos, key = { _, path -> path }) { index, photoPath ->
                    Box {
                        AsyncImage(
                            model = photoPath,
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

        if (enabled) {
            PhotoPickerButton(
                onPhotoPicked = { file ->
                    scope.launch { onAddPhoto(file.readBytes()) }
                },
                text = if (photos.isEmpty()) "Add Photo" else "Add Another Photo",
            )
        }
    }

    if (expandedPhotoIndex >= 0) {
        FullscreenImageViewer(
            photos = photos,
            initialIndex = expandedPhotoIndex,
            onDismiss = { expandedPhotoIndex = -1 },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridEmptyPreview() {
    LeafLogTheme {
        PhotoGrid(
            photos = emptyList(),
            onAddPhoto = {},
            onRemovePhoto = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridWithPhotosPreview() {
    LeafLogTheme {
        PhotoGrid(
            photos = listOf("/path/to/photo1.jpg", "/path/to/photo2.jpg"),
            onAddPhoto = {},
            onRemovePhoto = {},
        )
    }
}
