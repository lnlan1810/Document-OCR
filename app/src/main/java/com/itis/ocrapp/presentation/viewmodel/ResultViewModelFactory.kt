package com.itis.ocrapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.data.repository.OcrRepositoryImpl
import com.itis.ocrapp.data.repository.TranslationRepositoryImpl
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.data.source.TranslationDataSource
import com.itis.ocrapp.domain.usecase.ParsePassportUseCase
import com.itis.ocrapp.domain.usecase.TranslateTextUseCase

class ResultViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            val ocrDataSource = OcrDataSource()
            val ocrRepository = OcrRepositoryImpl(ocrDataSource, context.applicationContext)
            val parsePassportUseCase = ParsePassportUseCase(ocrRepository)
            val translationDataSource = TranslationDataSource()
            val translationRepository = TranslationRepositoryImpl(translationDataSource)
            val translateTextUseCase = TranslateTextUseCase(translationRepository)
            @Suppress("UNCHECKED_CAST")
            return ResultViewModel(parsePassportUseCase, translateTextUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}