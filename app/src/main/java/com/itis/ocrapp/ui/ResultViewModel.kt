package com.itis.ocrapp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.itis.ocrapp.ocr.PassportParser
import com.itis.ocrapp.utils.showToast
import java.text.Normalizer
import java.util.regex.Pattern

class ResultViewModel : ViewModel() {

    private val _resultText = MutableLiveData<String>()
    val resultText: LiveData<String> get() = _resultText

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private lateinit var originalFields: Map<String, String>
    private var targetLanguage: String = "vi"
    private lateinit var translator: Translator

    private val fieldLabels = mapOf(
        "vi" to mapOf(
            "Họ và tên" to "Họ và tên",
            "Ngày sinh" to "Ngày sinh",
            "Giới tính" to "Giới tính",
            "Ngày cấp" to "Ngày cấp",
            "Có giá trị đến" to "Có giá trị đến",
            "Số hộ chiếu" to "Số hộ chiếu",
            "Nơi sinh" to "Nơi sinh"
        ),
        "en" to mapOf(
            "Họ và tên" to "Full Name",
            "Ngày sinh" to "Date of Birth",
            "Giới tính" to "Gender",
            "Ngày cấp" to "Date of Issue",
            "Có giá trị đến" to "Expiry Date",
            "Số hộ chiếu" to "Passport Number",
            "Nơi sinh" to "Place of Birth"
        ),
        "ru" to mapOf(
            "Họ và tên" to "Фамилия и имя",
            "Ngày sinh" to "Дата рождения",
            "Giới tính" to "Пол",
            "Ngày cấp" to "Дата выдачи",
            "Có giá trị đến" to "Действителен до",
            "Số hộ chiếu" to "Номер паспорта",
            "Nơi sinh" to "Место рождения"
        )
    )

    private val vietnameseToRussian = mapOf(
        "a" to "а", "b" to "б", "c" to "к", "d" to "д", "đ" to "д",
        "e" to "е", "g" to "г", "h" to "х", "i" to "и",
        "k" to "к", "l" to "л", "m" to "м", "n" to "н",
        "o" to "о", "p" to "п", "q" to "к", "r" to "р", "s" to "с",
        "t" to "т", "u" to "у", "v" to "в", "x" to "кс", "y" to "и"
    )

    fun initialize(rawText: String) {
        val formattedText = PassportParser.parse(rawText)
        originalFields = parseFields(formattedText)
        _resultText.value = formattedText
        setupTranslator()
    }

    fun setTargetLanguage(position: Int) {
        targetLanguage = when (position) {
            0 -> "vi"
            1 -> TranslateLanguage.ENGLISH
            2 -> TranslateLanguage.RUSSIAN
            else -> "vi"
        }
        if (targetLanguage != "vi") setupTranslator()
        else displayFields(originalFields)
    }

    fun translateFields() {
        if (targetLanguage == "vi") {
            displayFields(originalFields)
            return
        }

        val translatedFields = mutableMapOf<String, String>()
        val fieldsToTranslate = originalFields.entries.toList()
        var completedTranslations = 0

        fieldsToTranslate.forEach { (key, value) ->
            when {
                key in listOf("Ngày sinh", "Ngày cấp", "Có giá trị đến", "Số hộ chiếu") -> {
                    translatedFields[key] = value
                    completedTranslations++
                    if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                }
                targetLanguage == TranslateLanguage.RUSSIAN && key in listOf("Họ và tên", "Nơi sinh") -> {
                    translatedFields[key] = transliterateVietnameseToRussian(value)
                    completedTranslations++
                    if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                }
                else -> {
                    translator.translate(value)
                        .addOnSuccessListener { translatedValue ->
                            translatedFields[key] = translatedValue
                            completedTranslations++
                            if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                        }
                        .addOnFailureListener { e ->
                            _toastMessage.value = "Ошибка перевода $key: ${e.message}"
                            translatedFields[key] = value
                            completedTranslations++
                            if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                        }
                }
            }
        }
    }

    private fun setupTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.VIETNAMESE)
            .setTargetLanguage(targetLanguage)
            .build()
        translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnFailureListener { e ->
                _toastMessage.value = "Ошибка загрузки модели перевода: ${e.message}"
            }
    }

    private fun parseFields(formattedText: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        formattedText.lines().forEach { line ->
            val parts = line.split(": ", limit = 2)
            if (parts.size == 2) fields[parts[0]] = parts[1]
        }
        return fields
    }

    private fun transliterateVietnameseToRussian(text: String): String {
        val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalizedText).replaceAll("")

        val words = withoutDiacritics.split(" ")
        val transliteratedWords = words.map { word ->
            val chars = word.lowercase()
            val result = StringBuilder()
            var i = 0
            while (i < chars.length) {
                val char = chars[i].toString()
                val mappedChar = vietnameseToRussian[char] ?: char
                result.append(mappedChar)
                i++
            }
            if (result.isNotEmpty()) result[0].uppercase() + result.substring(1) else ""
        }
        return transliteratedWords.joinToString(" ")
    }

    private fun displayFields(fields: Map<String, String>) {
        val labels = fieldLabels[targetLanguage] ?: fieldLabels["vi"]!!
        val result = StringBuilder()
        fields.forEach { (key, value) ->
            val label = labels[key] ?: key
            result.append("$label: $value\n")
        }
        _resultText.value = result.toString()
    }

    override fun onCleared() {
        super.onCleared()
        if (::translator.isInitialized) translator.close()
    }
}