package com.itis.ocrapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
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

        // Lấy và hiển thị ảnh chân dung
        val faceImagePath = intent.getStringExtra("FACE_IMAGE_PATH")
        if (faceImagePath != null) {
            val faceBitmap = BitmapFactory.decodeFile(faceImagePath)
            if (faceBitmap != null) {
                binding.faceImageView.setImageBitmap(faceBitmap)
                binding.faceImageView.visibility = View.VISIBLE
            }
        }

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