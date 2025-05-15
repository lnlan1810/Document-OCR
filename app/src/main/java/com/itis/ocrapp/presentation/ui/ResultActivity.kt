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
import androidx.lifecycle.lifecycleScope
import com.itis.ocrapp.databinding.ActivityResultBinding
import com.itis.ocrapp.presentation.viewmodel.ResultViewModel
import com.itis.ocrapp.presentation.viewmodel.ResultViewModelFactory
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.showToast
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: ResultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = ResultViewModelFactory(applicationContext)
        viewModel = ViewModelProvider(this, factory)[ResultViewModel::class.java]

        val rawText = intent.getStringExtra("SCANNED_TEXT") ?: "Нет доступных данных"
        val documentType = intent.getStringExtra("DOCUMENT_TYPE") ?: "passport"
        val faceImagePath = intent.getStringExtra("FACE_IMAGE_PATH")

        // Khởi tạo ViewModel và chạy song song hai tác vụ
        lifecycleScope.launch {
            // Tác vụ 1: Khởi tạo ViewModel và hiển thị văn bản kết quả
            val textProcessingDeferred = async {
                viewModel.initialize(rawText, documentType)
            }

            // Tác vụ 2: Giải mã và hiển thị hình ảnh khuôn mặt
            val faceProcessingDeferred = async {
                faceImagePath?.let {
                    try {
                        val encFile = File(it)
                        val tempFile = File(cacheDir, "temp_face_image.png")
                        EncryptionUtils.decryptFile(this@ResultActivity, encFile, tempFile)
                        val faceBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        tempFile.delete() // Xóa tệp tạm sau khi sử dụng
                        faceBitmap
                    } catch (e: Exception) {
                        runOnUiThread { showToast("Ошибка загрузки изображения лица: ${e.message}") }
                        null
                    }
                }
            }

            // Chờ cả hai tác vụ hoàn thành
            textProcessingDeferred.await()
            val faceBitmap = faceProcessingDeferred.await()

            // Hiển thị hình ảnh khuôn mặt nếu có
            faceBitmap?.let {
                runOnUiThread {
                    binding.faceImageView.setImageBitmap(it)
                    binding.faceImageView.visibility = View.VISIBLE
                }
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