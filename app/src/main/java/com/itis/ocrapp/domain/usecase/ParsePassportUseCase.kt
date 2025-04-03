package com.itis.ocrapp.domain.usecase

import com.itis.ocrapp.data.model.PassportData
import com.itis.ocrapp.domain.repository.OcrRepository

class ParsePassportUseCase(private val ocrRepository: OcrRepository) {
    suspend fun execute(rawText: String): PassportData {
        return ocrRepository.parsePassport(rawText)
    }
}