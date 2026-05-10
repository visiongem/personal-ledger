package io.github.visiongem.ledger.core.extensions

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage

// Project is Compose-first (spec §2.1, no XML view layer): we expose Compose-shaped
// helpers and skip an ImageView.load wrapper to avoid pulling in coil's view artifact.

@Composable
fun resPainterOrNull(@DrawableRes id: Int): Painter? =
    if (id == 0) null else painterResource(id)

@Composable
fun ResAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    @DrawableRes placeholder: Int = 0,
    @DrawableRes error: Int = 0,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = resPainterOrNull(placeholder),
        error = resPainterOrNull(error),
        contentScale = contentScale,
    )
}
