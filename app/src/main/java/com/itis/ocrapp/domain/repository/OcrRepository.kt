package com.itis.ocrapp.domain.repository

import com.itis.ocrapp.data.model.DocumentData

interface OcrRepository {
    suspend fun recognizeTextFromCamera(imageProxy: androidx.camera.core.ImageProxy): Triple<String, String?, String?>

    suspend fun recognizeTextFromGallery(uri: android.net.Uri): Triple<String, String?, String?>

    suspend fun parseDocument(rawText: String, documentType: String): DocumentData
}