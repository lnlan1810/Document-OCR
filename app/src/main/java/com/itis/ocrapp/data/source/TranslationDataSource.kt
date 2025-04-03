package com.itis.ocrapp.data.source

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class TranslationDataSource {

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        return Translation.getClient(options)
    }

    suspend fun translateText(text: String, targetLang: String): String {
        val translator = getTranslator(TranslateLanguage.VIETNAMESE, targetLang)
        translator.downloadModelIfNeeded().await()
        return translator.translate(text).await()
    }

    suspend fun ensureModelDownloaded(targetLang: String) {
        val translator = getTranslator(TranslateLanguage.VIETNAMESE, targetLang)
        translator.downloadModelIfNeeded().await()
    }
}