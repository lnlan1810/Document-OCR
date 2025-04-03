package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.data.model.PassportData
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.repository.OcrRepository
import com.itis.ocrapp.utils.ImageUtils
import java.io.File

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

    override suspend fun parsePassport(rawText: String): PassportData {
        val lines = rawText.split("\n").map { it.trim() }
        var fullName: String? = null
        var dateOfBirth: String? = null
        var sex: String? = null
        var dateOfIssue: String? = null
        var dateOfExpiry: String? = null
        var passportNo: String? = null
        var placeOfBirth: String? = null

        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.contains("Họ và tên") || line.contains("Full name") || line.contains("name") -> {
                    fullName = lines.getOrNull(i + 1) ?: line.replace("Họ và tên", "").trim()
                }
                line.contains("Ngày sinh") || line.contains("Date of birth") -> {
                    dateOfBirth = lines.getOrNull(i + 1) ?: extractDate(line)
                }
                line.contains("Giới tính") || line.contains("Sex") -> {
                    sex = extractSex(lines.getOrNull(i + 1) ?: line)
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

        return PassportData(fullName, dateOfBirth, sex, dateOfIssue, dateOfExpiry, passportNo, placeOfBirth)
    }

    private fun extractDate(text: String): String? = Regex("\\d{2}/\\d{2}/\\d{4}").find(text)?.value

    private fun extractSex(text: String): String? {
        return when {
            text.contains("Nữ") || text.contains("E") || text.contains("F") -> "Nữ"
            text.contains("Nam") || text.contains("M") -> "Nam"
            else -> null
        }
    }

    private fun extractPassportNo(text: String): String? = Regex("[A-Z]\\d{7}").find(text)?.value
}