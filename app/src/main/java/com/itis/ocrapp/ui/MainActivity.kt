package com.itis.ocrapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.databinding.ActivityMainBinding
import com.itis.ocrapp.utils.showToast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    var galleryUri: Uri? = null // Để dùng trong ViewModel

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) viewModel.checkCameraPermission(this) }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { galleryUri = it; viewModel.processImageFromGallery(it, this) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initialize(this, binding)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.toastMessage.observe(this) { message ->
            showToast(message)
        }
        viewModel.navigateToResult.observe(this) { (text, facePath) ->
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("SCANNED_TEXT", text)
                facePath?.let { putExtra("FACE_IMAGE_PATH", it) }
            }
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        binding.scanButton.setOnClickListener {
            viewModel.checkCameraPermission(this)
            if (shouldRequestPermission()) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.pickImageButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun shouldRequestPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }
}