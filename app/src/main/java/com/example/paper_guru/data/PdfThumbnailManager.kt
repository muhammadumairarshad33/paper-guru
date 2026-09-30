package com.example.paper_guru.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfThumbnailManager {

    // 1. In-memory LRU Cache (Fast path)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of memory

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // 2. Disk Cache directory in app cache
    private fun getDiskCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "pdf_thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getDiskCacheFile(context: Context, key: String): File {
        val safeKey = key.replace(Regex("[^a-zA-Z0-9_]"), "_") + ".png"
        return File(getDiskCacheDir(context), safeKey)
    }

    suspend fun getThumbnail(
        context: Context,
        paperKey: String,
        assetPath: String?,
        remoteUrl: String?,
        isAvailable: Boolean
    ): Bitmap? {
        if (!isAvailable) return null

        // Step 1: Memory cache lookup (instant)
        memoryCache.get(paperKey)?.let {
            return it
        }

        // Step 2: Disk cache lookup (very fast)
        val diskFile = getDiskCacheFile(context, paperKey)
        if (diskFile.exists() && diskFile.length() > 0) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(diskFile.absolutePath)
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                memoryCache.put(paperKey, bitmap)
                return bitmap
            }
        }

        // Step 3: Render first page on IO thread & store in disk cache
        return withContext(Dispatchers.IO) {
            try {
                var pfd: ParcelFileDescriptor? = null
                var tempFileToDelete: File? = null

                if (assetPath != null) {
                    val temp = File(context.cacheDir, "thumb_tmp_${paperKey.hashCode()}.pdf")
                    context.assets.open(assetPath).use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFileToDelete = temp
                    pfd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
                } else if (remoteUrl != null) {
                    val cachedRemote = File(context.cacheDir, "cached_" + remoteUrl.hashCode() + ".pdf")
                    if (cachedRemote.exists() && cachedRemote.length() > 0) {
                        pfd = ParcelFileDescriptor.open(cachedRemote, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }

                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    try {
                        if (renderer.pageCount > 0) {
                            val page = renderer.openPage(0)
                            // Compact thumbnail dimensions to keep memory low and scrolling fast
                            val thumbWidth = 140
                            val thumbHeight = (thumbWidth * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(160, 210)
                            val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.RGB_565)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()

                            // Save to disk cache for future instant reloads
                            try {
                                FileOutputStream(diskFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                                }
                            } catch (_: Exception) {
                                // Ignore disk write error
                            }

                            // Store in memory cache
                            memoryCache.put(paperKey, bitmap)
                            return@withContext bitmap
                        }
                    } finally {
                        renderer.close()
                        pfd.close()
                        tempFileToDelete?.delete()
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun clearAll(context: Context) {
        memoryCache.evictAll()
        val dir = getDiskCacheDir(context)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}

@Composable
fun PdfThumbnailView(
    paperKey: String,
    assetPath: String?,
    remoteUrl: String?,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(paperKey) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(paperKey) { mutableStateOf(isAvailable) }

    LaunchedEffect(paperKey, isAvailable) {
        if (isAvailable) {
            val result = PdfThumbnailManager.getThumbnail(context, paperKey, assetPath, remoteUrl, isAvailable)
            bitmap = result
            isLoading = false
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Paper Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = if (isAvailable) MaterialTheme.colorScheme.primary else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
