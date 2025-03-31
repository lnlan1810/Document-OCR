package com.itis.ocrapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.itis.ocrapp.databinding.ActivityResultBinding
import com.itis.ocrapp.ocr.PassportParser
import com.itis.ocrapp.utils.showToast

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rawText = intent.getStringExtra("SCANNED_TEXT") ?: "Không có dữ liệu"
        val formattedText = PassportParser.parse(rawText)
        binding.resultText.text = formattedText

        binding.copyButton.setOnClickListener {
            copyToClipboard(formattedText)
            showToast("Đã sao chép văn bản")
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Text", text)
        clipboard.setPrimaryClip(clip)
    }
}