package io.github.visiongem.ledger.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

fun File.readTextSafely(charset: Charset = Charsets.UTF_8): String? = try {
    readText(charset)
} catch (_: IOException) {
    null
}

fun File.writeTextSafely(content: String, charset: Charset = Charsets.UTF_8): Boolean = try {
    parentFile?.takeIf { !it.exists() }?.mkdirs()
    writeText(content, charset)
    true
} catch (_: IOException) {
    false
}

fun Context.saveImageToGallery(
    bitmap: Bitmap,
    displayName: String,
    relativePath: String = Environment.DIRECTORY_PICTURES,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90,
): Uri? {
    val mimeType = when (format) {
        Bitmap.CompressFormat.PNG -> "image/png"
        Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
        else -> "image/jpeg"
    }
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
    }
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null
    return try {
        contentResolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(format, quality, out)) {
                throw IOException("Bitmap compression failed")
            }
        } ?: throw IOException("ContentResolver.openOutputStream returned null")
        uri
    } catch (_: IOException) {
        contentResolver.delete(uri, null, null)
        null
    }
}

fun Context.uriToFile(uri: Uri, destination: File? = null): File? {
    val target = destination ?: File(cacheDir, "uri-${System.currentTimeMillis()}")
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        target
    } catch (_: IOException) {
        null
    }
}
