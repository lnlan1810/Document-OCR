package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.data.model.DocumentData
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.repository.OcrRepository
import com.itis.ocrapp.utils.ImageUtils
import java.io.File
import java.text.Normalizer

class OcrRepositoryImpl(
    private val ocrDataSource: OcrDataSource,
    private val context: Context
) : OcrRepository {

    @OptIn(ExperimentalGetImage::class)
    override suspend fun recognizeTextFromCamera(imageProxy: androidx.camera.core.ImageProxy): Pair<String, String?> {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val text = ocrDataSource.recognizeText(inputImage)
        val bitmap = ImageUtils.getBitmapFromInputImage(inputImage, context)
        val facePath = bitmap?.let { detectAndCropFace(inputImage, it) }
        imageProxy.close()
        return Pair(text, facePath)
    }

    override suspend fun recognizeTextFromGallery(uri: Uri): Pair<String, String?> {
        val inputImage = InputImage.fromFilePath(context, uri)
        val text = ocrDataSource.recognizeText(inputImage)
        val bitmap = ImageUtils.getBitmapFromUri(uri, context)
        val facePath = bitmap?.let { detectAndCropFace(inputImage, it) }
        return Pair(text, facePath)
    }

    private suspend fun detectAndCropFace(inputImage: InputImage, bitmap: Bitmap): String? {
        val faces = ocrDataSource.detectFaces(inputImage)
        return if (faces.isNotEmpty()) {
            val faceBitmap = ocrDataSource.cropFaceBitmap(bitmap, faces[0])
            val file = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.png")
            faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(file))
            file.absolutePath
        } else null
    }

    override suspend fun parseDocument(rawText: String, documentType: String): DocumentData {

        val lines = rawText.split("\n").map {
            it.trim()
                .replace("\\s+".toRegex(), " ")
                .replace(":", "")
                .replace("：", "")
        }.filter { it.isNotBlank() }

        return when (documentType) {
            "passport" -> parsePassport(lines)
            "citizen_id" -> parseCitizenId(lines)
            else -> DocumentData(documentType = documentType)
        }
    }

    private fun parsePassport(lines: List<String>): DocumentData {
        Log.d("OcrRepositoryImpl", "Raw text lines: ${lines.joinToString("")}")

        var fullName: String? = null
        var dateOfBirth: String? = null
        var sex: String? = null
        var dateOfIssue: String? = null
        var dateOfExpiry: String? = null
        var documentNumber: String? = null
        var placeOfBirth: String? = null
        var idCardNumber: String? = null
        var address: String? = null
        var licenseClass: String? = null
        var placeOfOrigin: String? = null

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            Log.d("OcrRepositoryImpl", "Processing line $i: $line")
            when {
                fullName == null && (line.contains("họ và tên") || line.contains("tên") || line.contains("full name") || line.contains("ho và tên")) -> {
                    fullName = lines.getOrNull(i + 1)?.trim() ?: ""
                    Log.d("OcrRepositoryImpl", "Full name extracted: $fullName")
                }
                dateOfBirth == null && (line.contains("ngày sinh") || line.contains("date of birth")) -> {
                    dateOfBirth = extractDate(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "Date of birth extracted: $dateOfBirth")
                }
                sex == null && (line.contains("giới tính") || line.contains("sex")) -> {
                    sex = extractSex(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "Sex extracted: $sex")
                }
                dateOfIssue == null && (line.contains("ngày cấp") || line.contains("date of issue")) -> {
                    dateOfIssue = extractDate(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "Date of issue extracted: $dateOfIssue")
                }
                dateOfExpiry == null && (line.contains("có giá trị đến") || line.contains("date of expiry")) -> {
                    dateOfExpiry = extractDate(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "Date of expiry extracted: $dateOfExpiry")
                }
                documentNumber == null && (line.contains("số hộ chiếu") || line.contains("passport no") || line.contains("số giấy phép") || line.contains("số căn cước") || line.contains("document no")) -> {
                    documentNumber = extractDocumentNumber(getNextValidLine(lines, i + 1) ?: line, "passport")
                    Log.d("OcrRepositoryImpl", "Document number extracted: $documentNumber")
                }
                placeOfBirth == null && (line.contains("nơi sinh") || line.contains("place of birth") || line.contains("Noi sinh") || line.contains("NGi sinh") || line.contains("Ni sinh")) -> {
                    placeOfBirth = lines.getOrNull(i + 1)?.trim() ?: ""
                    Log.d("OcrRepositoryImpl", "Place of birth extracted: $placeOfBirth")
                }
                idCardNumber == null && (line.contains("số cmnd") || line.contains("id card") || line.contains("cmnd")) -> {
                    idCardNumber = extractIdCardNumber(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "ID card number extracted: $idCardNumber")
                }
                address == null && (line.contains("địa chỉ") || line.contains("address")) -> {
                    address = lines.getOrNull(i + 1)?.trim() ?: ""
                    Log.d("OcrRepositoryImpl", "Address extracted: $address")
                }
                licenseClass == null && (line.contains("hạng") || line.contains("class")) -> {
                    licenseClass = extractLicenseClass(getNextValidLine(lines, i + 1) ?: line)
                    Log.d("OcrRepositoryImpl", "License class extracted: $licenseClass")
                }
                placeOfOrigin == null && (line.contains("quê quán") || line.contains("place of origin")) -> {
                    placeOfOrigin = lines.getOrNull(i + 1)?.trim() ?: ""
                    Log.d("OcrRepositoryImpl", "Place of origin extracted: $placeOfOrigin")
                }

            }
        }

        return DocumentData(
            documentType = "passport",
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            sex = sex,
            dateOfIssue = dateOfIssue,
            dateOfExpiry = dateOfExpiry,
            documentNumber = documentNumber,
            placeOfBirth = placeOfBirth,
            idCardNumber = idCardNumber,
            address = address,
            licenseClass = licenseClass,
            placeOfOrigin = placeOfOrigin,
        )
    }

    private fun parseCitizenId(lines: List<String>): DocumentData {
        Log.d("OcrRepositoryImpl", "Raw text lines: ${lines.joinToString("\n")}")

        var fullName: String? = null
        var dateOfBirth: String? = null
        var sex: String? = null
        var documentNumber: String? = null
        var placeOfBirth: String? = null
        var address: String? = null
        var placeOfOrigin: String? = null
        var nationality: String? = null

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            Log.d("OcrRepositoryImpl", "Processing citizen ID line $i: $line")
            when {
                fullName == null && (line.contains("họ và tên") || line.contains("ho va ten") || line.contains("tên") || line.contains("full name") || line.contains("ho và tên")) -> {
                    fullName = lines.getOrNull(i + 1)?.trim() ?: ""
                    Log.d("OcrRepositoryImpl", "Full name extracted: $fullName")
                }
                dateOfBirth == null && (line.contains("ngay sinh") || line.contains("date of birth") || line.contains("ngày sinh") || line.contains("date of bith")) -> {
                    val dateText = extractValue(line, listOf("ngay sinh", "date of birth"), lines.getOrNull(i + 1)) ?: line
                    dateOfBirth = extractDate(dateText)
                    Log.d("OcrRepositoryImpl", "Date of birth extracted: $dateOfBirth")
                }
                sex == null && (line.contains("gioi tinh") || line.contains("sex")) -> {
                    val sexText = extractValue(line, listOf("gioi tinh", "sex"), lines.getOrNull(i + 1)) ?: line
                    sex = extractSex(sexText)
                    Log.d("OcrRepositoryImpl", "Sex extracted: $sex")
                }
                documentNumber == null && (line.contains("so") || line.contains("no.") || line.contains("số i no.") || line.contains("số")) -> {
                    val numberText = extractValue(line, listOf("so", "no.", "số", "số i no.", "no"), lines.getOrNull(i + 1)) ?: line
                    documentNumber = extractDocumentNumber(numberText, "citizen_id")
                    Log.d("OcrRepositoryImpl", "Document number extracted: $documentNumber")
                }
                address == null && (line.contains("noi thuong tru") || line.contains("place of residence") || line.contains("noi thuờng trú")) -> {
                    address = extractValue(line, listOf("noi thuong tru", "place of residence", "noi thuờng trú", "nơi thuờng trú", "/", "noi thường trú", "|"), lines.getOrNull(i + 1))
                    Log.d("OcrRepositoryImpl", "Address extracted: $address")
                }
                placeOfOrigin == null && (line.contains("que quan") || line.contains("place of origin") || line.contains("quê quán")) -> {
                    placeOfOrigin = extractValue(line, listOf("que quan", "quê quán", "place of origin", "place of oigin", "/i", "/", "|", "place of onigin"), lines.getOrNull(i + 1))
                    placeOfOrigin = placeOfOrigin?.replace("que quan", "", ignoreCase = true)
                        ?.replace("place of origin", "", ignoreCase = true)?.trim()
                    Log.d("OcrRepositoryImpl", "Place of origin extracted: $placeOfOrigin")
                }
                nationality == null && (line.contains("quoc tich") || line.contains("quốc tịch") || line.contains("nationality") || line.contains("nationalfty")) -> {
                    nationality = extractValue(line, listOf("quoc tich", "nationality", "quốc tịch", "/", "nationalfty"), lines.getOrNull(i + 1))
                    Log.d("OcrRepositoryImpl", "Nationality extracted: $nationality")
                }
            }
        }

        return DocumentData(
            documentType = "citizen_id",
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            sex = sex,
            documentNumber = documentNumber,
            placeOfBirth = placeOfBirth,
            address = address,
            placeOfOrigin = placeOfOrigin,
            nationality = nationality
        )
    }

    private fun extractValueFromSameLine(line: String, keywords: List<String>): String? {
        var cleanedLine = line
        keywords.forEach { keyword ->
            cleanedLine = cleanedLine.replace(keyword, "", ignoreCase = true)
        }
        cleanedLine = cleanedLine.trim()

        // Nếu có dấu ":" hoặc có nhiều khoảng trắng thì split
        val parts = cleanedLine.split(Regex("[:\\s]{2,}"))
        return if (parts.size >= 2) parts[1].trim() else null
    }


    private fun extractValue(line: String, keywords: List<String>, nextLine: String? = null): String? {
        var value = line
        keywords.forEach { keyword ->
            value = value.replace(keyword, "", ignoreCase = true).trim()
        }
        // Kiểm tra giá trị có hợp lệ không (ít nhất 3 ký tự và không chứa từ khóa)
        return if (value.isNotBlank() && value.length > 2 && !keywords.any { value.lowercase().contains(it) }) {
            value
        } else {
            // Thử lấy giá trị từ dòng tiếp theo nếu có
            nextLine?.takeIf { it.isNotBlank() && it.length > 2 }
        }
    }

    private fun extractDate(text: String): String? {
        val normalizedText = text.replace("o", "0", ignoreCase = true)
        val datePattern = Regex("\\b(\\d{1,2})[\\s/|.-]*(\\d{1,2})[\\s/|.-]*(\\d{4})\\b")
        val match = datePattern.find(normalizedText)
        return match?.let {
            val day = it.groupValues[1].padStart(2, '0')
            val month = it.groupValues[2].padStart(2, '0')
            val year = it.groupValues[3]
            if (isValidDate(day.toIntOrNull(), month.toIntOrNull(), year.toIntOrNull())) {
                "$day/$month/$year"
            } else {
                null
            }
        }
    }

    private fun isValidDate(day: Int?, month: Int?, year: Int?): Boolean {
        if (day == null || month == null || year == null) return false
        if (month < 1 || month > 12) return false
        if (day < 1 || day > 31) return false
        if (year < 1900 || year > 9999) return false
        val maxDays = when (month) {
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        return day <= maxDays
    }

    private fun extractSex(text: String): String? {
        val normalizedText = text.lowercase()
        return when {
            normalizedText.contains("nu") || normalizedText.contains("nữ") || normalizedText.contains("female") || normalizedText.contains("f") -> "Nữ"
            normalizedText.contains("nam") || normalizedText.contains("male") || normalizedText.contains("m") -> "Nam"
            else -> null
        }
    }

    private fun extractDocumentNumber(text: String, documentType: String): String? {
        return when (documentType) {
            "passport" -> {
                val passportPattern = Regex("[A-Z]\\d{7}")
                passportPattern.find(text)?.value
            }
            "citizen_id" -> {
                val idPattern = Regex("\\b\\d{9}(\\d{3})?\\b")
                idPattern.find(text)?.value
            }
            else -> null
        }
    }

    private fun extractIdCardNumber(text: String): String? {
        val idCardPattern = Regex("\\b\\d{9}(\\d{3})?\\b")
        return idCardPattern.find(text)?.value
    }

    private fun extractLicenseClass(text: String): String? {
        val classPattern = Regex("\\b[A-F]\\d?\\b")
        return classPattern.find(text)?.value
    }

    private fun getNextValidLine(lines: List<String>, startIndex: Int, excludeKeywords: List<String> = emptyList()): String? {
        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.isNotBlank() && line.length > 2 && !line.lowercase().containsAny(excludeKeywords)) {
                return line
            }
        }
        return null
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        val lowerCase = this.lowercase()
        return keywords.any { lowerCase.contains(it) }
    }
}