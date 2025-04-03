package com.itis.ocrapp.domain.repository

interface TranslationRepository {
    suspend fun translateText(text: String, targetLanguage: String): String
    suspend fun ensureModelDownloaded(targetLanguage: String)
}