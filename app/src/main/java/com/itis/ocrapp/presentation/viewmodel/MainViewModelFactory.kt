package com.itis.ocrapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.data.repository.OcrRepositoryImpl
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.usecase.ProcessImageUseCase

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val ocrDataSource = OcrDataSource()
            val ocrRepository = OcrRepositoryImpl(ocrDataSource, context)
            val processImageUseCase = ProcessImageUseCase(ocrRepository)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(processImageUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}