package com.itis.ocrapp.utils

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