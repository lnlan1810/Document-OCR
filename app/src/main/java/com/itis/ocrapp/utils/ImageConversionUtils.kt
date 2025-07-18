package com.itis.ocrapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas

object ImageConversionUtils {
    fun convertToArgb8888(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.isHardwareAccelerated()) {
            return bitmap
        }
        val argbBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(argbBitmap)
        try {
            val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }
            canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageConversionUtils", "Error converting bitmap to ARGB_8888: ${e.message}")
        }
        return argbBitmap
    }

    fun Bitmap.isHardwareAccelerated(): Boolean {
        return this.config == Bitmap.Config.HARDWARE
    }
}