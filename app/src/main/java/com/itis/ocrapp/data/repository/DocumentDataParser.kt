package com.itis.ocrapp.data.repository

import android.util.Log
import com.itis.ocrapp.data.model.DocumentData
import com.itis.ocrapp.utils.TextProcessingUtils
import com.itis.ocrapp.utils.TextProcessingUtils.containsAny
import com.itis.ocrapp.utils.TextProcessingUtils.extractDate
import com.itis.ocrapp.utils.TextProcessingUtils.extractDocumentNumber
import com.itis.ocrapp.utils.TextProcessingUtils.extractIdCardNumber
import com.itis.ocrapp.utils.TextProcessingUtils.extractLicenseClass
import com.itis.ocrapp.utils.TextProcessingUtils.extractSex
import com.itis.ocrapp.utils.TextProcessingUtils.extractValue

class DocumentDataParser {
    private val TAG = "DocumentDataParser"

    suspend fun parseDocument(rawText: String, documentType: String): DocumentData {
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
            placeOfOrigin = placeOfOrigin
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
                address == null && (line.contains("noi thuong tru") || line.contains("place of residence") || line.contains("noi thuờng trú") || line.contains("nơi thuờng trú")) -> {
                    address = extractValue(line, listOf("noi thuong tru", "place of residence", "noi thuờng trú", "ergus thuờng trú", "/", "noi thường trú", "|"), lines.getOrNull(i + 1))
                    Log.d("OcrRepositoryImpl", "Address extracted: $address")
                }
                placeOfOrigin == null && (line.contains("que quan") || line.contains("place of origin") || line.contains("quê quán")) -> {
                    placeOfOrigin = extractValue(line, listOf("que quan", "quê quán", "place of origin", "place of oigin", "/i", "/", "|", "l", "place of onigin"), lines.getOrNull(i + 1))
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

    private fun getNextValidLine(lines: List< String>, startIndex: Int, excludeKeywords: List<String> = emptyList()): String? {
        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.isNotBlank() && line.length > 2 && !line.lowercase().containsAny(excludeKeywords)) {
                return line
            }
        }
        return null
    }
}