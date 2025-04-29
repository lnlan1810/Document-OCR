package com.itis.ocrapp.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.databinding.ActivityMainBinding
import com.itis.ocrapp.presentation.viewmodel.MainViewModel
import com.itis.ocrapp.presentation.viewmodel.MainViewModelFactory
import com.itis.ocrapp.utils.showToast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var selectedDocumentType: String = "passport"

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) viewModel.checkCameraPermission(this) }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.processGalleryImage(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, MainViewModelFactory(this))[MainViewModel::class.java]
        viewModel.initialize(this, binding)

        setupDocumentTypeSpinner()
        setupObservers()
        setupListeners()
    }

    private fun setupDocumentTypeSpinner() {
        val documentTypes = arrayOf("паспорт", "удостоверение личности гражданина")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, documentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.documentTypeSpinner.adapter = adapter

        binding.documentTypeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedDocumentType = when (position) {
                    0 -> "passport"
                    1 -> "citizen_id"
                    else -> "passport"
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                selectedDocumentType = "passport"
            }
        }
    }

    private fun setupObservers() {
        viewModel.toastMessage.observe(this) { message ->
            showToast(message)
        }
        viewModel.navigateToResult.observe(this) { (text, facePath) ->
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("SCANNED_TEXT", text)
                putExtra("DOCUMENT_TYPE", selectedDocumentType)
                facePath?.let { putExtra("FACE_IMAGE_PATH", it) }
            }
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        binding.scanButton.setOnClickListener {
            if (shouldRequestPermission()) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            else viewModel.checkCameraPermission(this)
        }

        binding.pickImageButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun shouldRequestPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }
}