package com.itis.ocrapp.domain.usecase

import com.itis.ocrapp.domain.repository.TranslationRepository

class TranslateTextUseCase(private val translationRepository: TranslationRepository) {
    suspend fun execute(text: String, targetLanguage: String): String {
        return translationRepository.translateText(text, targetLanguage)
    }

    suspend fun ensureModelDownloaded(targetLanguage: String) {
        translationRepository.ensureModelDownloaded(targetLanguage)
    }
}