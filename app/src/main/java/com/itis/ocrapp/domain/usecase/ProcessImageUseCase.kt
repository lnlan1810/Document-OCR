package com.itis.ocrapp.domain.usecase

import android.net.Uri
import com.itis.ocrapp.domain.repository.OcrRepository

class ProcessImageUseCase(private val ocrRepository: OcrRepository) {

    suspend fun processCameraImage(imageProxy: androidx.camera.core.ImageProxy): Pair<String, String?> {
        return ocrRepository.recognizeTextFromCamera(imageProxy)
    }

    suspend fun processGalleryImage(uri: Uri): Pair<String, String?> {
        return ocrRepository.recognizeTextFromGallery(uri)
    }
}