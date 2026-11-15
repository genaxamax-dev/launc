package com.example.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.CockpitSurfaceBright
import com.example.ui.theme.TextSecondary

private object AppIconBitmapCache {
    private val cache = LruCache<Int, ImageBitmap>(64)

    fun getOrCreate(drawable: Drawable): ImageBitmap? {
        val key = System.identityHashCode(drawable)
        cache.get(key)?.let { return it }
        return runCatching {
            drawable.toBitmap(width = 72, height = 72).asImageBitmap()
        }.getOrNull()?.also { bmp ->
            cache.put(key, bmp)
        }
    }
}

@Composable
fun DrawableAppIcon(
    drawable: Drawable?,
    contentDescription: String,
    size: Dp = 48.dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val bitmap: ImageBitmap? = remember(drawable) {
        drawable?.let { AppIconBitmapCache.getOrCreate(it) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(CockpitSurfaceBright.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = contentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}
