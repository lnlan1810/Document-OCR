package com.itis.ocrapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.itis.ocrapp.camera.CameraManager
import com.itis.ocrapp.databinding.ActivityMainBinding
import com.itis.ocrapp.ocr.TextRecognizerManager
import com.itis.ocrapp.utils.ImageUtils
import com.itis.ocrapp.utils.showToast
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

class MainViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var context: Context

    private val _scannedText = MutableLiveData<String>()
    val scannedText: LiveData<String> get() = _scannedText

    private val _faceBitmap = MutableLiveData<Bitmap?>()
    val faceBitmap: LiveData<Bitmap?> get() = _faceBitmap

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateToResult = MutableLiveData<Pair<String, String?>>()
    val navigateToResult: LiveData<Pair<String, String?>> get() = _navigateToResult

    lateinit var cameraManager: CameraManager
    private lateinit var textRecognizer: TextRecognizerManager

    fun initialize(context: MainActivity, binding: ActivityMainBinding) {
        this.context = context
        cameraManager = CameraManager(context, context, binding, executor) { image ->
            processCameraImage(image)
        }
        textRecognizer = TextRecognizerManager(
            onSuccess = {},
            onFailure = { _toastMessage.value = it },
            onSuccessWithSource = { text, isFromGallery ->
                if (isFromGallery) {
                    processTextAndFaceFromGallery(text, context)
                }
            }
        )
    }

    fun checkCameraPermission(context: MainActivity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera()
        } else {
            _toastMessage.value = "Cần cấp quyền camera"
        }
    }

    fun processImageFromGallery(uri: Uri, context: MainActivity) {
        viewModelScope.launch {
            val inputImage = InputImage.fromFilePath(context, uri)
            textRecognizer.processImage(inputImage, isFromGallery = true)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processCameraImage(imageProxy: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val bitmap = ImageUtils.getBitmapFromInputImage(inputImage, context)
                if (bitmap == null) {
                    showResult(visionText.text, null)
                    _toastMessage.value = "Невозможно получить фотографию для распознавания лиц"
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                detectFaces(inputImage, bitmap, visionText.text) { imageProxy.close() }
            }
            .addOnFailureListener { e ->
                _toastMessage.value = "Ошибка распознавания: ${e.message}"
                imageProxy.close()
            }
    }

    private fun processTextAndFaceFromGallery(text: String, context: MainActivity) {
        val uri = (context as MainActivity).galleryUri ?: return
        val inputImage = InputImage.fromFilePath(context, uri)
        val bitmap = ImageUtils.getBitmapFromUri(uri, context)
        if (bitmap == null) {
            showResult(text, null)
            _toastMessage.value = "Невозможно получить фотографию из галереи для распознавания лиц"
            return
        }

        detectFaces(inputImage, bitmap, text)
    }

    private fun detectFaces(inputImage: InputImage, bitmap: Bitmap, text: String, onComplete: (() -> Unit)? = null) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        val detector = FaceDetection.getClient(options)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val faceBitmap = cropFaceBitmap(bitmap, face)
                    showResult(text, faceBitmap)
                } else {
                    showResult(text, null)
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                _toastMessage.value = "Ошибка распознавания лица: ${e.message}"
                showResult(text, null)
                onComplete?.invoke()
            }
    }

    private fun cropFaceBitmap(bitmap: Bitmap, face: Face): Bitmap {
        val paddingHorizontal = (face.boundingBox.width() * 0.4).toInt()
        val paddingVertical = (face.boundingBox.height() * 0.4).toInt()

        val left = (face.boundingBox.left - paddingHorizontal).coerceAtLeast(0)
        val top = (face.boundingBox.top - paddingVertical).coerceAtLeast(0)
        var width = (face.boundingBox.width() + 2 * paddingHorizontal).coerceAtMost(bitmap.width - left)
        var height = (face.boundingBox.height() + 2 * paddingVertical).coerceAtMost(bitmap.height - top)

        val targetAspectRatio = 4.0 / 5.0
        if (width.toDouble() / height > targetAspectRatio) {
            width = (height * targetAspectRatio).toInt()
        } else {
            height = (width / targetAspectRatio).toInt()
        }

        width = width.coerceAtMost(bitmap.width - left)
        height = height.coerceAtMost(bitmap.height - top)

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun showResult(text: String, faceBitmap: Bitmap?) {
        _scannedText.value = text
        _faceBitmap.value = faceBitmap

        val filePath = faceBitmap?.let {
            val file = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.png")
            it.compress(Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(file))
            file.absolutePath
        }
        _navigateToResult.value = Pair(text, filePath)
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}