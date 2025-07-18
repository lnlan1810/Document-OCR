package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.ImageProcessingUtils
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class DocumentDetector(private val context: Context) {
    private val TAG = "DocumentDetector"

    suspend fun detectAndCropDocument(bitmap: Bitmap): Pair<String?, String?> {
        try {
            val enhancedBitmap = ImageProcessingUtils.enhanceImageQuality(bitmap)
            val inputImage = InputImage.fromBitmap(enhancedBitmap, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
            val detector = FaceDetection.getClient(options)
            val faces = detector.process(inputImage).await()

            val croppedBitmap = if (faces.isNotEmpty()) {
                val face = faces[0]
                val rect = face.boundingBox
                val margin = 3
                val width = rect.width()
                val height = rect.height()
                val centerX = rect.centerX()
                val centerY = rect.centerY()
                val rightExpansion = (width * 5.5).toInt()
                val leftExpansion = (width * 1).toInt()
                val left = (rect.left - leftExpansion - margin).coerceAtLeast(0)
                val right = (rect.right + rightExpansion + margin).coerceAtMost(bitmap.width)
                val verticalExpansion = (height * 1.6).toInt()
                val top = (centerY - height / 2 - verticalExpansion - margin).coerceAtLeast(0)
                val bottom = (centerY + height / 2 + verticalExpansion + margin).coerceAtMost(bitmap.height)

                if (right > left && bottom > top) {
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top).also {
                        android.util.Log.d(TAG, "Document cropped via face detection: left=$left, top=$top, width=${right - left}, height=${bottom - top}")
                    }
                } else {
                    cropByEdgeDetection(bitmap)
                }
            } else {
                android.util.Log.w(TAG, "No face detected, attempting edge-based cropping")
                cropByEdgeDetection(bitmap)
            }

            enhancedBitmap.recycle()

            val documentFile = File(context.cacheDir, "document_image_${System.currentTimeMillis()}.enc")
            val tempDocumentFile = File(context.cacheDir, "temp_document_image.png")
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(tempDocumentFile))
            EncryptionUtils.encryptFile(context, tempDocumentFile, documentFile)
            tempDocumentFile.delete()

            val originalFile = File(context.cacheDir, "original_image_${System.currentTimeMillis()}.enc")
            val tempOriginalFile = File(context.cacheDir, "temp_original_image.png")
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(tempOriginalFile))
            EncryptionUtils.encryptFile(context, tempOriginalFile, originalFile)
            tempOriginalFile.delete()

            if (croppedBitmap != bitmap) {
                croppedBitmap.recycle()
            }

            return Pair(documentFile.absolutePath, originalFile.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error detecting document: ${e.message}")
            return Pair(null, null)
        }
    }

    private fun cropByEdgeDetection(bitmap: Bitmap): Bitmap {
        val grayBitmap = ImageProcessingUtils.convertToGrayscale(bitmap)
        val width = grayBitmap.width
        val height = grayBitmap.height
        val threshold = 200

        var left = 0
        var right = width - 1
        var top = 0
        var bottom = height - 1

        for (x in 0 until width) {
            var foundContent = false
            for (y in 0 until height) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                left = x
                break
            }
        }

        for (x in width - 1 downTo 0) {
            var foundContent = false
            for (y in 0 until height) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                right = x
                break
            }
        }

        for (y in 0 until height) {
            var foundContent = false
            for (x in 0 until width) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                top = y
                break
            }
        }

        for (y in height - 1 downTo 0) {
            var foundContent = false
            for (x in 0 until width) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                bottom = y
                break
            }
        }

        val margin = 10
        left = (left - margin).coerceAtLeast(0)
        top = (top - margin).coerceAtLeast(0)
        right = (right + margin).coerceAtMost(width)
        bottom = (bottom + margin).coerceAtMost(height)

        grayBitmap.recycle()

        return if (right > left && bottom > top) {
            Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top).also {
                android.util.Log.d(TAG, "Edge-based cropping: left=$left, top=$top, width=${right - left}, height=${bottom - top}")
            }
        } else {
            android.util.Log.w(TAG, "Edge detection failed, returning original bitmap")
            bitmap
        }
    }
}