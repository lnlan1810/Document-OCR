package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.ImageProcessingUtils
import com.itis.ocrapp.utils.ImageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CameraOcrProcessor(
    private val ocrDataSource: OcrDataSource,
    private val documentDetector: DocumentDetector,
    private val faceDetector: FaceDetector,
    private val context: Context
) {
    private val TAG = "CameraOcrProcessor"

    @OptIn(ExperimentalGetImage::class)
    suspend fun recognizeTextFromCamera(imageProxy: ImageProxy): Triple<String, String?, String?> = coroutineScope {
        val startTime = System.currentTimeMillis()

        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val colorBitmap = ImageUtils.getBitmapFromInputImage(inputImage, context)
        val grayscaleBitmap = colorBitmap?.let { ImageProcessingUtils.convertToGrayscale(it) }

        val textRecognitionDeferred = async {
            try {
                val enhancedInputImage = grayscaleBitmap?.let { InputImage.fromBitmap(it, imageProxy.imageInfo.rotationDegrees) } ?: inputImage
                ocrDataSource.recognizeText(enhancedInputImage)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error recognizing text: ${e.message}")
                ""
            }
        }

        val faceDetectionDeferred = async {
            try {
                colorBitmap?.let { faceDetector.detectAndCropFace(inputImage, it) }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detecting face: ${e.message}")
                null
            }
        }

        val documentDetectionDeferred = async {
            try {
                colorBitmap?.let { documentDetector.detectAndCropDocument(it) }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detecting document: ${e.message}")
                null
            }
        }

        val text = textRecognitionDeferred.await()
        val facePath = faceDetectionDeferred.await()
        val documentResult = documentDetectionDeferred.await()
        val (documentPath, originalPath) = documentResult ?: Pair(null, null)

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        android.util.Log.d(TAG, "Время выполнения распознавания с камеры: $duration мс")

        colorBitmap?.recycle()
        grayscaleBitmap?.recycle()
        imageProxy.close()
        Triple(text, facePath, originalPath)
    }
}