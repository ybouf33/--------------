package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
  private const val MAX_DIMENSION_PX = 640
  private const val MAX_BASE64_CHARS = 120_000

  fun uriToBoundedBase64(context: Context, uri: Uri): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
      val scaled = scaleBitmap(original, MAX_DIMENSION_PX)
      var quality = 70
      var encoded = ""
      while (quality >= 25) {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val candidate = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        if (candidate.length <= MAX_BASE64_CHARS) {
          encoded = candidate
          break
        }
        quality -= 15
      }
      if (scaled !== original) scaled.recycle()
      original.recycle()
      encoded.ifBlank { null }
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    if (base64.isBlank()) return null
    return try {
      val clean = base64.substringAfter("base64,", base64)
      val bytes = Base64.decode(clean, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val longest = max(width, height)
    if (longest <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / longest.toFloat()
    val newW = (width * ratio).toInt().coerceAtLeast(1)
    val newH = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
  }
}
