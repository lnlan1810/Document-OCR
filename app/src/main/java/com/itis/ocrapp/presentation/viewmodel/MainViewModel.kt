package com.itis.ocrapp.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itis.ocrapp.camera.CameraManager
import com.itis.ocrapp.domain.usecase.ProcessImageUseCase
import com.itis.ocrapp.utils.showToast
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainViewModel(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraManager: CameraManager
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage
    private val _navigateToResult = MutableLiveData<Triple<String, String?, String?>>()
    val navigateToResult: LiveData<Triple<String, String?, String?>> get() = _navigateToResult

    fun initialize(context: Context, binding: com.itis.ocrapp.databinding.ActivityMainBinding) {
        cameraManager = CameraManager(context, context as com.itis.ocrapp.presentation.ui.MainActivity, binding, executor) { image ->
            processCameraImage(image)
        }
    }

    fun checkCameraPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera()
        } else {
            _toastMessage.value = "Требуется разрешение на камеру"
        }
    }

    fun processGalleryImage(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val (text, facePath, documentPath) = processImageUseCase.processGalleryImage(uri)
                _navigateToResult.value = Triple(text, facePath, documentPath)
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка обработки изображения: ${e.message}"
            }
        }
    }

    private fun processCameraImage(imageProxy: androidx.camera.core.ImageProxy) {
        viewModelScope.launch {
            try {
                val (text, facePath, documentPath) = processImageUseCase.processCameraImage(imageProxy)
                _navigateToResult.value = Triple(text, facePath, documentPath)
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка обработки изображения: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}