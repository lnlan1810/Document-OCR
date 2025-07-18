package com.itis.ocrapp.domain.repository

import android.net.Uri
import androidx.camera.core.ImageProxy
import com.itis.ocrapp.data.model.DocumentData

interface OcrRepository {
    suspend fun recognizeTextFromCamera(imageProxy: ImageProxy): Triple<String, String?, String?>
    suspend fun recognizeTextFromGallery(uri: Uri): Triple<String, String?, String?>
    suspend fun parseDocument(rawText: String, documentType: String): DocumentData
}