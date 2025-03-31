package com.itis.ocrapp.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.itis.ocrapp.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val binding: ActivityMainBinding,
    private val executor: ExecutorService,
    private val onImageCaptured: (ImageProxy) -> Unit
) {

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetResolution(CameraConfig.HIGH_RESOLUTION)
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(CameraConfig.HIGH_RESOLUTION)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                binding.scanButton.setOnClickListener {
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                onImageCaptured(image)
                                // Không đóng image ở đây, để MainActivity xử lý
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // Xử lý lỗi
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                // Xử lý lỗi
            }
        }, ContextCompat.getMainExecutor(context))
    }
}