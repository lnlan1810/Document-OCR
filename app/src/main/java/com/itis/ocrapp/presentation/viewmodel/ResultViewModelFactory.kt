package com.itis.ocrapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.data.repository.CameraOcrProcessor
import com.itis.ocrapp.data.repository.DocumentDataParser
import com.itis.ocrapp.data.repository.DocumentDetector
import com.itis.ocrapp.data.repository.FaceDetector
import com.itis.ocrapp.data.repository.GalleryOcrProcessor
import com.itis.ocrapp.data.repository.OcrRepositoryImpl
import com.itis.ocrapp.data.repository.TranslationRepositoryImpl
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.data.source.TranslationDataSource
import com.itis.ocrapp.domain.repository.OcrRepository
import com.itis.ocrapp.domain.usecase.ParsePassportUseCase
import com.itis.ocrapp.domain.usecase.TranslateTextUseCase

class ResultViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            val ocrDataSource = OcrDataSource()
            val documentDetector = DocumentDetector(context.applicationContext)
            val faceDetector = FaceDetector(ocrDataSource, context.applicationContext)
            val cameraOcrProcessor = CameraOcrProcessor(ocrDataSource, documentDetector, faceDetector, context.applicationContext)
            val galleryOcrProcessor = GalleryOcrProcessor(ocrDataSource, documentDetector, faceDetector, context.applicationContext)
            val documentDataParser = DocumentDataParser()
            val ocrRepository: OcrRepository = OcrRepositoryImpl(cameraOcrProcessor, galleryOcrProcessor, documentDataParser, context.applicationContext)
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