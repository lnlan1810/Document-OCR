package com.itis.ocrapp.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.itis.ocrapp.databinding.ActivityResultBinding
import com.itis.ocrapp.presentation.viewmodel.ResultViewModel
import com.itis.ocrapp.presentation.viewmodel.ResultViewModelFactory
import com.itis.ocrapp.utils.showToast

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: ResultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = ResultViewModelFactory(applicationContext)
        viewModel = ViewModelProvider(this, factory)[ResultViewModel::class.java]

        val rawText = intent.getStringExtra("SCANNED_TEXT") ?: "Không có dữ liệu"
        val documentType = intent.getStringExtra("DOCUMENT_TYPE") ?: "passport"
        viewModel.initialize(rawText, documentType)

        val faceImagePath = intent.getStringExtra("FACE_IMAGE_PATH")
        faceImagePath?.let {
            val faceBitmap = BitmapFactory.decodeFile(it)
            if (faceBitmap != null) {
                binding.faceImageView.setImageBitmap(faceBitmap)
                binding.faceImageView.visibility = View.VISIBLE
            }
        }

        setupObservers()
        setupListeners()
        setupLanguageSpinner()
    }

    private fun setupObservers() {
        viewModel.resultText.observe(this) { text ->
            binding.resultText.text = text
        }
        viewModel.toastMessage.observe(this) { message ->
            showToast(message)
        }
    }

    private fun setupListeners() {
        binding.translateButton.setOnClickListener {
            viewModel.translateFields()
        }
        binding.copyButton.setOnClickListener {
            copyToClipboard(binding.resultText.text.toString())
            showToast("Текст скопирован")
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.btnCreatePdf.setOnClickListener {
            val intent = Intent(this, PDFPreviewActivity::class.java).apply {
                putExtra("DOCUMENT_IMAGE_PATH", intent.getStringExtra("DOCUMENT_IMAGE_PATH"))
                putExtra("TRANSLATION_TEXT", binding.resultText.text.toString())
            }
            startActivity(intent)
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("Вьетнамский", "Английский", "Русский")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.setTargetLanguage(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Text", text)
        clipboard.setPrimaryClip(clip)
    }
}