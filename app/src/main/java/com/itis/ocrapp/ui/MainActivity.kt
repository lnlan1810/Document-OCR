package com.itis.ocrapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.itis.ocrapp.camera.CameraManager
import com.itis.ocrapp.databinding.ActivityMainBinding
import com.itis.ocrapp.ocr.TextRecognizerManager
import com.itis.ocrapp.utils.ImageUtils
import com.itis.ocrapp.utils.showToast
import java.io.File
import java.io.FileOutputStream
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
            onSuccess = { text -> /* Không dùng nữa */ },
            onFailure = { showToast(it) },
            onSuccessWithSource = { text, isFromGallery ->
                if (isFromGallery) {
                    processTextAndFaceFromGallery(text)
                } else {
                    showToast("Xử lý camera đã được chuyển vào callback")
                }
            }
        )
    }

    private var galleryUri: Uri? = null // Lưu Uri từ gallery

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
        this.galleryUri = null // Đặt lại Uri khi dùng camera
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Nhận diện văn bản trước
        textRecognizer.recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val bitmap = ImageUtils.getBitmapFromInputImage(inputImage, this)
                if (bitmap == null) {
                    showResult(visionText.text, null)
                    showToast("Không thể lấy ảnh để phát hiện khuôn mặt")
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                // Phát hiện khuôn mặt
                val options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .build()

                val detector = FaceDetection.getClient(options)
                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val face = faces[0] // Lấy khuôn mặt đầu tiên
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

// Đảm bảo vùng cắt không vượt quá kích thước bitmap
                            width = width.coerceAtMost(bitmap.width - left)
                            height = height.coerceAtMost(bitmap.height - top)

                            val faceBitmap = Bitmap.createBitmap(
                                bitmap,
                                left,
                                top,
                                width,
                                height
                            )
                            showResult(visionText.text, faceBitmap)
                        } else {
                            showResult(visionText.text, null) // Không tìm thấy khuôn mặt
                        }
                        imageProxy.close() // Đóng ImageProxy sau khi xử lý xong
                    }
                    .addOnFailureListener { e ->
                        showToast("Lỗi phát hiện khuôn mặt: ${e.message}")
                        showResult(visionText.text, null)
                        imageProxy.close() // Đóng ImageProxy nếu có lỗi
                    }
            }
            .addOnFailureListener { e ->
                showToast("Lỗi nhận diện: ${e.message}")
                imageProxy.close() // Đóng ImageProxy nếu có lỗi
            }
    }

    private fun processImage(uri: Uri) {
        this.galleryUri = uri // Lưu Uri từ gallery
        val inputImage = InputImage.fromFilePath(this, uri)
        textRecognizer.processImage(inputImage, isFromGallery = true)
    }

    private fun processTextAndFaceFromGallery(text: String) {
        val uri = galleryUri ?: return
        val inputImage = InputImage.fromFilePath(this, uri)
        val bitmap = ImageUtils.getBitmapFromUri(uri, this)
        if (bitmap == null) {
            showResult(text, null)
            showToast("Không thể lấy ảnh từ gallery để phát hiện khuôn mặt")
            return
        }

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()

        val detector = FaceDetection.getClient(options)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0] // Lấy khuôn mặt đầu tiên
                    // Mở rộng vùng cắt để bao gồm tóc, cổ và vai
                    val paddingHorizontal = (face.boundingBox.width() * 0.4).toInt() // Thêm 80% chiều rộng
                    val paddingVertical = (face.boundingBox.height() * 0.4).toInt() // Thêm 120% chiều cao

                    val left = (face.boundingBox.left - paddingHorizontal).coerceAtLeast(0)
                    val top = (face.boundingBox.top - paddingVertical).coerceAtLeast(0)
                    var width = (face.boundingBox.width() +  2 *paddingHorizontal).coerceAtMost(bitmap.width - left)
                    var height = (face.boundingBox.height() + 2 *paddingVertical).coerceAtMost(bitmap.height - top)

                    // Giữ tỷ lệ khung hình 4:5 (phù hợp với ảnh chân dung)
                    val targetAspectRatio = 4.0 / 5.0
                    if (width.toDouble() / height > targetAspectRatio) {
                        width = (height * targetAspectRatio).toInt()
                    } else {
                        height = (width / targetAspectRatio).toInt()
                    }

                    // Đảm bảo vùng cắt không vượt quá kích thước bitmap
                    width = width.coerceAtMost(bitmap.width - left)
                    height = height.coerceAtMost(bitmap.height - top)

                    val faceBitmap = Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        width,
                        height
                    )
                    showResult(text, faceBitmap)
                } else {
                    showResult(text, null) // Không tìm thấy khuôn mặt
                }
            }
            .addOnFailureListener { e ->
                showToast("Lỗi phát hiện khuôn mặt: ${e.message}")
                showResult(text, null)
            }
    }

    private fun showResult(text: String, faceBitmap: Bitmap?) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("SCANNED_TEXT", text)
            faceBitmap?.let {
                // Lưu Bitmap vào file tạm
                val file = File(cacheDir, "face_image_${System.currentTimeMillis()}.png")
                it.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
                putExtra("FACE_IMAGE_PATH", file.absolutePath)
            }
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}