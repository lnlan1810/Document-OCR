package com.itis.ocrapp.data.repository

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.itis.ocrapp.data.model.DocumentData
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.repository.CameraOcrRepository
import com.itis.ocrapp.domain.repository.DocumentParser
import com.itis.ocrapp.domain.repository.GalleryOcrRepository
import com.itis.ocrapp.domain.repository.OcrRepository

class OcrRepositoryImpl(
    private val cameraOcrProcessor: CameraOcrProcessor,
    private val galleryOcrProcessor: GalleryOcrProcessor,
    private val documentDataParser: DocumentDataParser,
    context: Context
) : OcrRepository {

    override suspend fun recognizeTextFromCamera(imageProxy: ImageProxy): Triple<String, String?, String?> {
        return cameraOcrProcessor.recognizeTextFromCamera(imageProxy)
    }

    override suspend fun recognizeTextFromGallery(uri: Uri): Triple<String, String?, String?> {
        return galleryOcrProcessor.recognizeTextFromGallery(uri)
    }

    override suspend fun parseDocument(rawText: String, documentType: String): DocumentData {
        return documentDataParser.parseDocument(rawText, documentType)
    }
}