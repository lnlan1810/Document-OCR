package com.itis.ocrapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.camera.CameraManager
import com.itis.ocrapp.databinding.ActivityMainBinding
import com.itis.ocrapp.ocr.TextRecognizerManager
import com.itis.ocrapp.utils.showToast
import com.itis.ocrapp.utils.startActivityWithData
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val executor = Executors.newSingleThreadExecutor()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) cameraManager.startCamera() }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { processImage(it) } }

    private val cameraManager by lazy {
        CameraManager(this, this, binding, executor) { image ->
            processCameraImage(image)
        }
    }

    private val textRecognizer by lazy {
        TextRecognizerManager(
            onSuccess = { showResult(it) },
            onFailure = { showToast(it) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.startCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.pickImageButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processCameraImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.processImage(inputImage)
    }

    private fun processImage(uri: Uri) {
        val inputImage = InputImage.fromFilePath(this, uri)
        textRecognizer.processImage(inputImage)
    }

    private fun showResult(text: String) {
        startActivityWithData(ResultActivity::class.java, "SCANNED_TEXT", text)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}