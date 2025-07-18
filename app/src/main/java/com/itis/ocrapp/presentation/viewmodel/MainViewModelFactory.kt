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
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.usecase.ProcessImageUseCase

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val ocrDataSource = OcrDataSource()
            val documentDetector = DocumentDetector(context)
            val faceDetector = FaceDetector(ocrDataSource, context)
            val cameraOcrProcessor = CameraOcrProcessor(ocrDataSource, documentDetector, faceDetector, context)
            val galleryOcrProcessor = GalleryOcrProcessor(ocrDataSource, documentDetector, faceDetector, context)
            val documentDataParser = DocumentDataParser()
            val ocrRepository = OcrRepositoryImpl(cameraOcrProcessor, galleryOcrProcessor, documentDataParser, context)
            val processImageUseCase = ProcessImageUseCase(ocrRepository)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(processImageUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}