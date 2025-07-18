package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.ImageConversionUtils
import com.itis.ocrapp.utils.ImageProcessingUtils
import com.itis.ocrapp.utils.ImageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.FileOutputStream

class GalleryOcrProcessor(
    private val ocrDataSource: OcrDataSource,
    private val documentDetector: DocumentDetector,
    private val faceDetector: FaceDetector,
    private val context: Context
) {
    private val TAG = "GalleryOcrProcessor"

    suspend fun recognizeTextFromGallery(uri: Uri): Triple<String, String?, String?> = coroutineScope {
        val startTime = System.currentTimeMillis()

        val inputImage = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating InputImage from URI: ${e.message}")
            return@coroutineScope Triple("", null, null)
        }

        var bitmap = ImageUtils.getBitmapFromUri(uri, context)?.let { ImageConversionUtils.convertToArgb8888(it) }

        val textRecognitionDeferred = async {
            try {
                val enhancedBitmap = bitmap?.let { ImageProcessingUtils.enhanceImageQuality(it) }
                val enhancedInputImage = enhancedBitmap?.let { InputImage.fromBitmap(it, inputImage.rotationDegrees) } ?: inputImage
                ocrDataSource.recognizeText(enhancedInputImage)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error recognizing text: ${e.message}")
                ""
            }
        }

        val faceDetectionDeferred = async {
            try {
                bitmap?.let { faceDetector.detectAndCropFace(inputImage, it) }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detecting face: ${e.message}")
                null
            }
        }

        val documentDetectionDeferred = async {
            try {
                bitmap?.let { documentDetector.detectAndCropDocument(it) }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detecting document: ${e.message}")
                null
            }
        }

        val text = textRecognitionDeferred.await()
        val facePath = faceDetectionDeferred.await()
        val documentPath = documentDetectionDeferred.await()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        android.util.Log.d(TAG, "Время выполнения распознавания из галереи: $duration мс")

        bitmap?.recycle()
        Triple(text, facePath, documentPath?.first)
    }
}