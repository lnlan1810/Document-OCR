package com.itis.ocrapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.itis.ocrapp.databinding.ActivityResultBinding
import com.itis.ocrapp.ocr.PassportParser
import com.itis.ocrapp.utils.showToast
import java.text.Normalizer
import java.util.regex.Pattern

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var originalFields: Map<String, String>
    private var targetLanguage: String = "vi" // Default to Vietnamese ("vi")
    private lateinit var translator: Translator

    // Predefined field labels in Vietnamese, English, and Russian
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

    // Vietnamese to Russian transliteration mapping (simplified, base characters only)
    private val vietnameseToRussian = mapOf(
        "a" to "а",
        "b" to "б", "c" to "к", "d" to "д", "đ" to "д",
        "e" to "е",
        "g" to "г", "h" to "х", "i" to "и",
        "k" to "к", "l" to "л", "m" to "м", "n" to "н",
        "o" to "о",
        "p" to "п", "q" to "к", "r" to "р", "s" to "с",
        "t" to "т", "u" to "у",
        "v" to "в", "x" to "кс", "y" to "и",

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rawText = intent.getStringExtra("SCANNED_TEXT") ?: "Нет доступных данных"
        val formattedText = PassportParser.parse(rawText)
        originalFields = parseFields(formattedText)
        binding.resultText.text = formattedText

        val faceImagePath = intent.getStringExtra("FACE_IMAGE_PATH")
        if (faceImagePath != null) {
            val faceBitmap = BitmapFactory.decodeFile(faceImagePath)
            if (faceBitmap != null) {
                binding.faceImageView.setImageBitmap(faceBitmap)
                binding.faceImageView.visibility = View.VISIBLE
            }
        }

        setupLanguageSpinner()
        setupTranslator()

        binding.translateButton.setOnClickListener {
            translateFields()
        }

        binding.copyButton.setOnClickListener {
            copyToClipboard(binding.resultText.text.toString())
            showToast("Текст скопирован")
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("Вьетнамский", "Английский", "Русский")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                targetLanguage = when (position) {
                    0 -> "vi"
                    1 -> TranslateLanguage.ENGLISH
                    2 -> TranslateLanguage.RUSSIAN
                    else -> "vi"
                }
                if (targetLanguage != "vi") {
                    setupTranslator()
                } else {
                    displayFields(originalFields)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.VIETNAMESE)
            .setTargetLanguage(targetLanguage)
            .build()

        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {}
            .addOnFailureListener { e ->
                showToast("Ошибка загрузки модели перевода: ${e.message}")
            }
    }

    private fun parseFields(formattedText: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        formattedText.lines().forEach { line ->
            val parts = line.split(": ", limit = 2)
            if (parts.size == 2) {
                fields[parts[0]] = parts[1]
            }
        }
        return fields
    }

    private fun translateFields() {
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
                    // No translation for dates and passport numbers
                    translatedFields[key] = value
                    completedTranslations++
                    if (completedTranslations == fieldsToTranslate.size) {
                        displayFields(translatedFields)
                    }
                }
                targetLanguage == TranslateLanguage.RUSSIAN && key in listOf("Họ và tên", "Nơi sinh") -> {
                    // Custom transliteration for Russian names and places
                    translatedFields[key] = transliterateVietnameseToRussian(value)
                    completedTranslations++
                    if (completedTranslations == fieldsToTranslate.size) {
                        displayFields(translatedFields)
                    }
                }
                else -> {
                    // Use ML Kit for other fields (e.g., gender)
                    translator.translate(value)
                        .addOnSuccessListener { translatedValue ->
                            translatedFields[key] = translatedValue
                            completedTranslations++
                            if (completedTranslations == fieldsToTranslate.size) {
                                displayFields(translatedFields)
                            }
                        }
                        .addOnFailureListener { e ->
                            showToast("Ошибка перевода $key: ${e.message}")
                            translatedFields[key] = value
                            completedTranslations++
                            if (completedTranslations == fieldsToTranslate.size) {
                                displayFields(translatedFields)
                            }
                        }
                }
            }
        }
    }

    private fun transliterateVietnameseToRussian(text: String): String {
        // Normalize text to remove diacritics
        val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutDiacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalizedText).replaceAll("")

        // Check if the normalized text matches a predefined place name
        vietnameseToRussian[withoutDiacritics]?.let { return it }

        // Split the text into words and transliterate each word
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
            // Capitalize the first letter of each word
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
        binding.resultText.text = result.toString()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Text", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::translator.isInitialized) {
            translator.close()
        }
    }
}