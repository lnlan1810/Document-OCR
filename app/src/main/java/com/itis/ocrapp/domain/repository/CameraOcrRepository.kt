package com.itis.ocrapp.domain.repository

import androidx.camera.core.ImageProxy

interface CameraOcrRepository {
    suspend fun recognizeTextFromCamera(imageProxy: ImageProxy): Triple<String, String?, String?>
}