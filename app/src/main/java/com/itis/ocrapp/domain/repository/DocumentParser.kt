package com.itis.ocrapp.domain.repository

import com.itis.ocrapp.data.model.DocumentData

interface DocumentParser {
    suspend fun parseDocument(rawText: String, documentType: String): DocumentData
}