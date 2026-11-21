package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.annotation.DrawableRes
import com.example.ui.components.resolvePortfolioMediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ImageDownloadResult(
    val success: Boolean,
    val fileName: String = "",
    val savedLocation: String = "",
    val errorMessage: String? = null
)

/**
 * Play-policy-compliant image downloader for STAR ARTIST STUDIO.
 * Saves bundled drawables, Base64 data URLs, content URIs, and remote HTTPS portfolio images
 * directly into the device gallery (`Pictures/StarArtistStudio`) using Scoped Storage MediaStore
 * with zero runtime storage permissions.
 */
object ImageDownloadHelper {

    fun buildDownloadFileName(title: String, mimeType: String = "image/jpeg"): String {
        val slug = title
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(48)
            .ifBlank { "portfolio_shot" }

        val ext = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        return "star_artist_${slug}.$ext"
    }

    fun decodeDataUrlImageBytes(dataUrl: String): Pair<ByteArray, String>? {
        if (!dataUrl.startsWith("data:image/", ignoreCase = true)) return null
        val commaIdx = dataUrl.indexOf(',')
        if (commaIdx <= 0) return null
        val header = dataUrl.substring(5, commaIdx) // e.g. image/jpeg;base64
        val mime = header.substringBefore(';').ifBlank { "image/jpeg" }
        val base64Part = dataUrl.substring(commaIdx + 1)
        return try {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            if (bytes.isEmpty()) null else bytes to mime
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun savePortfolioImageToGallery(
        context: Context,
        title: String,
        mediaUrl: String
    ): ImageDownloadResult = withContext(Dispatchers.IO) {
        try {
            val resolved = resolvePortfolioMediaModel(mediaUrl)
            val payload: Pair<ByteArray, String> = when {
                resolved is Int -> {
                    readDrawableBytes(context, resolved)
                        ?: return@withContext ImageDownloadResult(
                            success = false,
                            errorMessage = "Could not load studio artwork resource."
                        )
                }
                mediaUrl.startsWith("data:image/", ignoreCase = true) -> {
                    decodeDataUrlImageBytes(mediaUrl)
                        ?: return@withContext ImageDownloadResult(
                            success = false,
                            errorMessage = "Invalid encoded image data."
                        )
                }
                mediaUrl.startsWith("http://", ignoreCase = true) ||
                    mediaUrl.startsWith("https://", ignoreCase = true) -> {
                    downloadRemoteImageBytes(mediaUrl)
                        ?: return@withContext ImageDownloadResult(
                            success = false,
                            errorMessage = "Could not fetch image. Check your internet connection."
                        )
                }
                mediaUrl.startsWith("content://", ignoreCase = true) -> {
                    val bytes = context.contentResolver.openInputStream(Uri.parse(mediaUrl))?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) {
                        return@withContext ImageDownloadResult(
                            success = false,
                            errorMessage = "Could not read local image file."
                        )
                    }
                    val mime = context.contentResolver.getType(Uri.parse(mediaUrl)) ?: "image/jpeg"
                    bytes to mime
                }
                else -> {
                    return@withContext ImageDownloadResult(
                        success = false,
                        errorMessage = "Unsupported image source."
                    )
                }
            }

            val (bytes, mimeType) = payload
            val fileName = buildDownloadFileName(title, mimeType)
            persistToDevicePictures(context, bytes, fileName, mimeType)
        } catch (e: Exception) {
            ImageDownloadResult(
                success = false,
                errorMessage = e.message ?: "Unable to save image."
            )
        }
    }

    private fun readDrawableBytes(context: Context, @DrawableRes resId: Int): Pair<ByteArray, String>? {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return null
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.toByteArray() to "image/jpeg"
        } finally {
            bitmap.recycle()
        }
    }

    private fun downloadRemoteImageBytes(urlString: String): Pair<ByteArray, String>? {
        val conn = (URL(urlString).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode !in 200..299) return null
            val contentType = conn.contentType?.substringBefore(';')?.trim()
                ?.takeIf { it.startsWith("image/", ignoreCase = true) }
                ?: "image/jpeg"
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) null else bytes to contentType
        } finally {
            conn.disconnect()
        }
    }

    private fun persistToDevicePictures(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): ImageDownloadResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StarArtistStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return ImageDownloadResult(
                    success = false,
                    errorMessage = "Could not create gallery entry."
                )

            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: return ImageDownloadResult(
                    success = false,
                    errorMessage = "Could not write image stream."
                )

                val publishValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, publishValues, null, null)
                return ImageDownloadResult(
                    success = true,
                    fileName = fileName,
                    savedLocation = "Pictures/StarArtistStudio/$fileName"
                )
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val studioDir = File(dir, "StarArtistStudio").apply { mkdirs() }
            val outFile = File(studioDir, fileName)
            outFile.writeBytes(bytes)
            return ImageDownloadResult(
                success = true,
                fileName = fileName,
                savedLocation = outFile.absolutePath
            )
        }
    }
}
