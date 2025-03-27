package com.itis.ocrapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itis.ocrapp.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy chuỗi văn bản thô từ Intent
        val scannedText = intent.getStringExtra("SCANNED_TEXT") ?: "Không có dữ liệu"
        val formattedText = parsePassportText(scannedText)
        binding.resultText.text = formattedText

        binding.copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Scanned Text", formattedText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Đã sao chép văn bản", Toast.LENGTH_SHORT).show()
        }

        binding.backButton.setOnClickListener { finish() }
    }

    private fun parsePassportText(rawText: String): String {
        val result = StringBuilder()
        val lines = rawText.split("\n").map { it.trim() }

        // Các biến để lưu thông tin
        var fullName: String? = null
        var nationality: String? = null
        var dateOfBirth: String? = null
        var sex: String? = null
        var dateOfIssue: String? = null
        var placeOfIssue: String? = null
        var dateOfExpiry: String? = null
        var passportNo: String? = null
        var placeOfBirth: String? = null

        // Phân tích từng dòng để mô phỏng Text Blocks
        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.contains("Họ và tên") || line.contains("Full name") -> {
                    fullName = lines.getOrNull(i + 1) ?: line.replace("Họ và tên", "").replace("Full name", "").trim()
                }
                line.contains("Ngày sinh") || line.contains("Date of birth") -> {
                    dateOfBirth = lines.getOrNull(i + 1) ?: extractDate(line)
                }
                line.contains("Giới tính") || line.contains("Sex") -> {
                    sex = lines.getOrNull(i + 1) ?: extractSex(line)
                }
                line.contains("Ngày cấp") || line.contains("Date of issue") -> {
                    dateOfIssue = lines.getOrNull(i + 1) ?: extractDate(line)
                }
                line.contains("Có giá trị đến") || line.contains("Date of expiry") -> {
                    dateOfExpiry = lines.getOrNull(i + 1) ?: extractDate(line)
                }
                line.contains("Số hộ chiếu") || line.contains("Passport NO") -> {
                    passportNo = lines.getOrNull(i + 1) ?: extractPassportNo(line)
                }
                line.contains("Nơi sinh") || line.contains("Place of birth") -> {
                    placeOfBirth = lines.getOrNull(i + 1) ?: line.replace("Nơi sinh", "").replace("Place of birth", "").trim()
                }
            }
        }

        // Định dạng kết quả
        fullName?.let { result.append("Họ và tên: $it\n") }
        dateOfBirth?.let { result.append("Ngày sinh: $it\n") }
        sex?.let { result.append("Giới tính: $it\n") }
        dateOfIssue?.let { result.append("Ngày cấp: $it\n") }
        dateOfExpiry?.let { result.append("Có giá trị đến: $it\n") }
        passportNo?.let { result.append("Số hộ chiếu: $it\n") }
        placeOfBirth?.let { result.append("Nơi sinh: $it\n") }

        return if (result.isEmpty()) "Không tìm thấy thông tin" else result.toString()
    }

    private fun extractDate(text: String): String? {
        val datePattern = Regex("\\d{2}/\\d{2}/\\d{4}")
        return datePattern.find(text)?.value
    }

    private fun extractSex(text: String): String? {
        return when {
            text.contains("Nữ") || text.contains("E") -> "Nữ"
            text.contains("Nam") || text.contains("M") -> "Nam"
            else -> null
        }
    }

    private fun extractPassportNo(text: String): String? {
        val passportPattern = Regex("[A-Z]\\d{7}")
        return passportPattern.find(text)?.value
    }
}