package com.itis.ocrapp.data.repository

import com.itis.ocrapp.data.source.TranslationDataSource
import com.itis.ocrapp.domain.repository.TranslationRepository

class  TranslationRepositoryImpl(private val translationDataSource: TranslationDataSource) : TranslationRepository {

    override suspend fun translateText(text: String, targetLanguage: String): String {
        return translationDataSource.translateText(text, targetLanguage)
    }

    override suspend fun ensureModelDownloaded(targetLanguage: String) {
        translationDataSource.ensureModelDownloaded(targetLanguage)
    }
}