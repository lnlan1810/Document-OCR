package com.itis.ocrapp.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Extension để hiển thị Toast nhanh
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Extension để chuyển Activity với dữ liệu
fun AppCompatActivity.startActivityWithData(
    targetActivity: Class<*>,
    key: String,
    value: String
) {
    val intent = Intent(this, targetActivity).apply {
        putExtra(key, value)
    }
    startActivity(intent)
}

// Extension để kiểm tra chuỗi rỗng hoặc null
fun String?.ifEmptyOrNull(default: String): String {
    return if (this.isNullOrEmpty()) default else this
}

// Extension để định dạng ngày tháng từ chuỗi (ví dụ: "250320" -> "25/03/2020")
fun String.formatDateFromMrz(): String {
    if (length < 6) return this
    val year = substring(0, 2)
    val month = substring(2, 4)
    val day = substring(4, 6)
    return "$day/$month/20$year"
}

// Extension để kiểm tra và trích xuất số hộ chiếu từ chuỗi
fun String.extractPassportNumber(): String? {
    val passportPattern = Regex("[A-Z]\\d{7}")
    return passportPattern.find(this)?.value
}