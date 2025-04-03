package com.itis.ocrapp.data.source

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrDataSource {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    suspend fun recognizeText(image: InputImage): String {
        return textRecognizer.process(image).await().text
    }

    suspend fun detectFaces(image: InputImage): List<Face> {
        return faceDetector.process(image).await()
    }

    fun cropFaceBitmap(bitmap: Bitmap, face: Face): Bitmap {
        val paddingHorizontal = (face.boundingBox.width() * 0.4).toInt()
        val paddingVertical = (face.boundingBox.height() * 0.4).toInt()

        val left = (face.boundingBox.left - paddingHorizontal).coerceAtLeast(0)
        val top = (face.boundingBox.top - paddingVertical).coerceAtLeast(0)
        var width = (face.boundingBox.width() + 2 * paddingHorizontal).coerceAtMost(bitmap.width - left)
        var height = (face.boundingBox.height() + 2 * paddingVertical).coerceAtMost(bitmap.height - top)

        val targetAspectRatio = 4.0 / 5.0
        if (width.toDouble() / height > targetAspectRatio) width = (height * targetAspectRatio).toInt()
        else height = (width / targetAspectRatio).toInt()

        width = width.coerceAtMost(bitmap.width - left)
        height = height.coerceAtMost(bitmap.height - top)

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}