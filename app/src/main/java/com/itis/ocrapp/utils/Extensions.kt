package com.itis.ocrapp.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

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

fun String?.ifEmptyOrNull(default: String): String {
    return if (this.isNullOrEmpty()) default else this
}

fun String.formatDateFromMrz(): String {
    if (length < 6) return this
    val year = substring(0, 2)
    val month = substring(2, 4)
    val day = substring(4, 6)
    return "$day/$month/20$year"
}

fun String.extractPassportNumber(): String? {
    val passportPattern = Regex("[A-Z]\\d{7}")
    return passportPattern.find(this)?.value
}