package com.itis.ocrapp.domain.usecase

import android.net.Uri
import com.itis.ocrapp.domain.repository.OcrRepository

class ProcessImageUseCase(private val ocrRepository: OcrRepository) {

    suspend fun processCameraImage(imageProxy: androidx.camera.core.ImageProxy): Triple<String, String?, String?> {
        return ocrRepository.recognizeTextFromCamera(imageProxy)
    }

    suspend fun processGalleryImage(uri: Uri): Triple<String, String?, String?> {
        return ocrRepository.recognizeTextFromGallery(uri)

    }
}