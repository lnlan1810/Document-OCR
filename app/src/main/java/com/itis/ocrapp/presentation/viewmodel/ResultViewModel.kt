package com.itis.ocrapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.itis.ocrapp.domain.usecase.ParsePassportUseCase
import com.itis.ocrapp.domain.usecase.TranslateTextUseCase
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.regex.Pattern

class ResultViewModel(
    private val parsePassportUseCase: ParsePassportUseCase,
    private val translateTextUseCase: TranslateTextUseCase
) : ViewModel() {

    private val _resultText = MutableLiveData<String>()
    val resultText: LiveData<String> get() = _resultText

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private lateinit var originalFields: Map<String, String>
    private var targetLanguage: String = "vi"
    private var documentType: String = "passport"

    private val vietnameseToRussian = mapOf(
        "a" to "а", "b" to "б", "c" to "к", "d" to "д", "đ" to "д",
        "e" to "е", "g" to "г", "h" to "х", "i" to "и",
        "k" to "к", "l" to "л", "m" to "м", "n" to "н",
        "o" to "о", "p" to "п", "q" to "к", "r" to "р", "s" to "с",
        "t" to "т", "u" to "у", "v" to "в", "x" to "кс", "y" to "и"
    )

    fun initialize(rawText: String, documentType: String) {
        this.documentType = documentType
        viewModelScope.launch {
            val document = parsePassportUseCase.execute(rawText, documentType)
            originalFields = document.toMap()
            displayFields(originalFields)
            if (targetLanguage != "vi") ensureTranslationModel()
        }
    }

    fun setTargetLanguage(position: Int) {
        targetLanguage = when (position) {
            0 -> "vi"
            1 -> TranslateLanguage.ENGLISH
            2 -> TranslateLanguage.RUSSIAN
            else -> "vi"
        }
        viewModelScope.launch {
            if (targetLanguage != "vi") ensureTranslationModel()
            else displayFields(originalFields)
        }
    }

    fun translateFields() {
        if (targetLanguage == "vi") {
            viewModelScope.launch {
                displayFields(originalFields)
            }
            return
        }

        viewModelScope.launch {
            val translatedFields = mutableMapOf<String, String>()
            val fieldsToTranslate = originalFields.entries.toList()
            var completedTranslations = 0

            fieldsToTranslate.forEach { (key, value) ->
                when {
                    key in listOf("Ngày sinh", "Ngày cấp", "Có giá trị đến", "Số tài liệu", "Số CMND", "Hạng") -> {
                        translatedFields[key] = value
                        completedTranslations++
                        if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                    }
                    targetLanguage == TranslateLanguage.RUSSIAN && key in listOf("Tên đầy đủ", "Nơi sinh", "Địa chỉ", "Quê quán") -> {
                        translatedFields[key] = transliterateVietnameseToRussian(value)
                        completedTranslations++
                        if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                    }
                    else -> {
                        try {
                            val translatedValue = translateTextUseCase.execute(value, targetLanguage)
                            translatedFields[key] = translatedValue
                        } catch (e: Exception) {
                            _toastMessage.value = "Lỗi dịch $key: ${e.message}"
                            translatedFields[key] = value
                        }
                        completedTranslations++
                        if (completedTranslations == fieldsToTranslate.size) displayFields(translatedFields)
                    }
                }
            }
        }
    }

    private fun ensureTranslationModel() {
        viewModelScope.launch {
            try {
                translateTextUseCase.ensureModelDownloaded(targetLanguage)
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка загрузки модели перевода: ${e.message}"
            }
        }
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

    private suspend fun displayFields(fields: Map<String, String>) {
        val result = StringBuilder()
        fields.forEach { (key, value) ->
            val label = if (targetLanguage == "vi") {
                key
            } else {
                try {
                    translateTextUseCase.execute(key, targetLanguage)
                } catch (e: Exception) {
                    _toastMessage.value = "Lỗi dịch nhãn $key: ${e.message}"
                    key
                }
            }
            result.append("$label: $value\n")
        }
        _resultText.value = result.toString().trim()
    }

    private fun com.itis.ocrapp.data.model.DocumentData.toMap(): Map<String, String> {
        val fields = mutableMapOf(
            "Tên đầy đủ" to (fullName ?: ""),
            "Ngày sinh" to (dateOfBirth ?: ""),
            "Giới tính" to (sex ?: ""),
            "Ngày cấp" to (dateOfIssue ?: ""),
            "Có giá trị đến" to (dateOfExpiry ?: ""),
            "Số tài liệu" to (documentNumber ?: ""),
            "Nơi sinh" to (placeOfBirth ?: ""),
            "Số CMND" to (idCardNumber ?: ""),
            "Địa chỉ" to (address ?: ""),
            "Hạng" to (licenseClass ?: ""),
            "Quê quán" to (placeOfOrigin ?: ""),
        )
        return fields.filterValues { it.isNotEmpty() }
    }
}