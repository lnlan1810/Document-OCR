package com.itis.ocrapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

object ImageUtils {

    @OptIn(ExperimentalGetImage::class)
    fun getBitmapFromInputImage(inputImage: InputImage, context: Context): Bitmap? {
        return try {
            when {
                inputImage.bitmapInternal != null -> {
                    // Trường hợp InputImage được tạo từ Bitmap
                    inputImage.bitmapInternal
                }
                inputImage.mediaImage != null -> {
                    // Trường hợp InputImage được tạo từ MediaImage (camera)
                    val mediaImage = inputImage.mediaImage
                    Bitmap.createBitmap(
                        mediaImage!!.width,
                        mediaImage.height,
                        Bitmap.Config.ARGB_8888
                    ).also { bitmap ->
                        val buffer = mediaImage.planes[0].buffer
                        bitmap.copyPixelsFromBuffer(buffer)
                    }
                }
                else -> {
                    null
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}