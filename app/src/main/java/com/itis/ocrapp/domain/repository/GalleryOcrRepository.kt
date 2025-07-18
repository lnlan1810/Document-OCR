package com.itis.ocrapp.domain.repository

import android.net.Uri

interface GalleryOcrRepository {
    suspend fun recognizeTextFromGallery(uri: Uri): Triple<String, String?, String?>
}