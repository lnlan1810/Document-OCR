package com.itis.ocrapp.domain.usecase

import com.itis.ocrapp.data.model.DocumentData
import com.itis.ocrapp.domain.repository.OcrRepository

class ParsePassportUseCase(private val ocrRepository: OcrRepository) {
    suspend fun execute(rawText: String, documentType: String): DocumentData {
        return ocrRepository.parseDocument(rawText, documentType)
    }
}