package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.itis.ocrapp.data.model.DocumentData
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.domain.repository.OcrRepository
import com.itis.ocrapp.utils.ImageProcessingUtils
import com.itis.ocrapp.utils.ImageUtils
import com.itis.ocrapp.utils.EncryptionUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.regex.Pattern

class OcrRepositoryImpl(
    private val ocrDataSource: OcrDataSource,
    private val context: Context
) : OcrRepository {

    @OptIn(ExperimentalGetImage::class)
    override suspend fun recognizeTextFromCamera(imageProxy: androidx.camera.core.ImageProxy): Triple<String, String?, String?> = coroutineScope {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        var bitmap = ImageUtils.getBitmapFromInputImage(inputImage, context)
        bitmap = bitmap?.let { convertToArgb8888(it) }?.let { ImageProcessingUtils.enhanceImageQuality(it) }

        // Thực hiện ba tác vụ song song
        val textRecognitionDeferred = async {
            try {
                val enhancedInputImage = bitmap?.let { InputImage.fromBitmap(it, imageProxy.imageInfo.rotationDegrees) } ?: inputImage
                ocrDataSource.recognizeText(enhancedInputImage)
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error recognizing text: ${e.message}")
                ""
            }
        }

        val faceDetectionDeferred = async {
            try {
                bitmap?.let {
                    val faceFilePath = detectAndCropFace(inputImage, it)
                    faceFilePath?.let { path ->
                        val faceFile = File(path)
                        val encFaceFile = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.enc")
                        val tempFaceFile = File(context.cacheDir, "temp_face_image.png")
                        FileInputStream(faceFile).use { input ->
                            FileOutputStream(tempFaceFile).use { output -> input.copyTo(output) }
                        }
                        EncryptionUtils.encryptFile(context, tempFaceFile, encFaceFile)
                        tempFaceFile.delete()
                        faceFile.delete()
                        encFaceFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error detecting face: ${e.message}")
                null
            }
        }

        val documentDetectionDeferred = async {
            try {
                val croppedBitmap = bitmap?.let { detectAndCropDocument(it) } ?: bitmap
                val documentFile = File(context.cacheDir, "document_image_${System.currentTimeMillis()}.enc")
                val tempDocumentFile = File(context.cacheDir, "temp_document_image.png")
                croppedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(tempDocumentFile))
                EncryptionUtils.encryptFile(context, tempDocumentFile, documentFile)
                tempDocumentFile.delete()
                documentFile.absolutePath
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error detecting document: ${e.message}")
                null
            }
        }

        // Chờ tất cả các tác vụ hoàn thành và thu thập kết quả
        val text = textRecognitionDeferred.await()
        val facePath = faceDetectionDeferred.await()
        val documentPath = documentDetectionDeferred.await()

        imageProxy.close()
        Triple(text, facePath, documentPath)
    }

    override suspend fun recognizeTextFromGallery(uri: Uri): Triple<String, String?, String?> = coroutineScope {
        val inputImage = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            Log.e("OcrRepositoryImpl", "Error creating InputImage from URI: ${e.message}")
            return@coroutineScope Triple("", null, null)
        }

        var bitmap = ImageUtils.getBitmapFromUri(uri, context)?.let { convertToArgb8888(it) }

        // Thực hiện ba tác vụ song song
        val textRecognitionDeferred = async {
            try {
                val croppedBitmap = bitmap?.let { detectAndCropDocument(it) } ?: bitmap
                val enhancedBitmap = croppedBitmap?.let { ImageProcessingUtils.enhanceImageQuality(it) }
                val enhancedInputImage = enhancedBitmap?.let { InputImage.fromBitmap(it, inputImage.rotationDegrees) } ?: inputImage
                ocrDataSource.recognizeText(enhancedInputImage)
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error recognizing text: ${e.message}")
                ""
            }
        }

        val faceDetectionDeferred = async {
            try {
                bitmap?.let {
                    val faceFilePath = detectAndCropFace(inputImage, it)
                    faceFilePath?.let { path ->
                        val faceFile = File(path)
                        val encFaceFile = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.enc")
                        val tempFaceFile = File(context.cacheDir, "temp_face_image.png")
                        FileInputStream(faceFile).use { input ->
                            FileOutputStream(tempFaceFile).use { output -> input.copyTo(output) }
                        }
                        EncryptionUtils.encryptFile(context, tempFaceFile, encFaceFile)
                        tempFaceFile.delete()
                        faceFile.delete()
                        encFaceFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error detecting face: ${e.message}")
                null
            }
        }

        val documentDetectionDeferred = async {
            try {
                val croppedBitmap = bitmap?.let { detectAndCropDocument(it) } ?: bitmap
                val documentFile = File(context.cacheDir, "document_image_${System.currentTimeMillis()}.enc")
                val tempDocumentFile = File(context.cacheDir, "temp_document_image.png")
                croppedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(tempDocumentFile))
                EncryptionUtils.encryptFile(context, tempDocumentFile, documentFile)
                tempDocumentFile.delete()
                documentFile.absolutePath
            } catch (e: Exception) {
                Log.e("OcrRepositoryImpl", "Error detecting document: ${e.message}")
                null
            }
        }

        // Chờ tất cả các tác vụ hoàn thành và thu thập kết quả
        val text = textRecognitionDeferred.await()
        val facePath = faceDetectionDeferred.await()
        val documentPath = documentDetectionDeferred.await()

        Triple(text, facePath, documentPath)
    }

    private suspend fun detectAndCropDocument(bitmap: Bitmap): Bitmap? {
        try {
            // Tăng độ tương phản để cải thiện phát hiện cạnh
            val enhancedBitmap = ImageProcessingUtils.enhanceImageQuality(bitmap)
            val inputImage = InputImage.fromBitmap(enhancedBitmap, 0)

            // Sử dụng ObjectDetection để tìm đối tượng
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .build()
            val objectDetector = ObjectDetection.getClient(options)
            val detectedObjects = objectDetector.process(inputImage).await()

            var bestObject: DetectedObject? = null
            var maxScore = 0f

            // Tinh chỉnh tiêu chí chọn đối tượng
            for (obj in detectedObjects) {
                val rect = obj.boundingBox
                val area = rect.width() * rect.height()
                val aspectRatio = rect.width().toFloat() / rect.height()
                // Kiểm tra nội dung văn bản trong vùng để xác định tài liệu
                val regionBitmap = Bitmap.createBitmap(
                    enhancedBitmap,
                    rect.left.coerceAtLeast(0),
                    rect.top.coerceAtLeast(0),
                    rect.width().coerceIn(1, enhancedBitmap.width - rect.left),
                    rect.height().coerceIn(1, enhancedBitmap.height - rect.top)
                )
                val regionImage = InputImage.fromBitmap(regionBitmap, 0)
                val textResult = ocrDataSource.recognizeText(regionImage)
                val textLines = textResult.split("\n").filter { it.trim().isNotEmpty() }
                regionBitmap.recycle()

                // Tính điểm dựa trên diện tích, tỷ lệ khung hình, và số dòng văn bản
                val score = (area.toFloat() / (bitmap.width * bitmap.height)) * // Tỷ lệ diện tích
                        (if (aspectRatio in 0.5f..2.0f) 1.0f else 0.5f) * // Tỷ lệ khung hình (A4, thẻ ID, hộ chiếu)
                        (textLines.size.coerceAtMost(10) / 10.0f) // Mật độ văn bản

                if (score > maxScore &&
                    rect.width() > bitmap.width * 0.4 && // Tăng ngưỡng kích thước
                    rect.height() > bitmap.height * 0.4 &&
                    aspectRatio in 0.5f..2.0f) { // Mở rộng phạm vi tỷ lệ
                    maxScore = score
                    bestObject = obj
                }
            }

            bestObject?.let { obj ->
                val rect = obj.boundingBox
                // Thêm lề nhỏ để đảm bảo không cắt mất nội dung
                val margin = 10
                val left = (rect.left - margin).coerceAtLeast(0)
                val top = (rect.top - margin).coerceAtLeast(0)
                val right = (rect.right + margin).coerceAtMost(bitmap.width)
                val bottom = (rect.bottom + margin).coerceAtMost(bitmap.height)
                if (right > left && bottom > top) {
                    val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                    Log.d("OcrRepositoryImpl", "Document cropped: left=$left, top=$top, width=${right - left}, height=${bottom - top}")
                    return croppedBitmap
                }
            }

            Log.w("OcrRepositoryImpl", "No valid document detected, attempting edge-based cropping")
            // Dự phòng: Sử dụng phân tích cạnh đơn giản nếu ObjectDetection thất bại
            return cropByEdgeDetection(bitmap)
        } catch (e: Exception) {
            Log.e("OcrRepositoryImpl", "Error detecting and cropping document: ${e.message}")
            return bitmap
        }
    }

    private fun cropByEdgeDetection(bitmap: Bitmap): Bitmap {
        // Tạo bitmap xám để phân tích cạnh
        val grayBitmap = ImageProcessingUtils.convertToGrayscale(bitmap)
        val width = grayBitmap.width
        val height = grayBitmap.height

        // Quét từ các cạnh để tìm vùng có nội dung
        var left = 0
        var right = width - 1
        var top = 0
        var bottom = height - 1

        // Ngưỡng pixel để xác định nội dung (giả sử nền sáng, nội dung tối)
        val threshold = 200 // Giá trị xám từ 0 (đen) đến 255 (trắng)

        // Tìm lề trái
        for (x in 0 until width) {
            var foundContent = false
            for (y in 0 until height) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                left = x
                break
            }
        }

        // Tìm lề phải
        for (x in width - 1 downTo 0) {
            var foundContent = false
            for (y in 0 until height) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                right = x
                break
            }
        }

        // Tìm lề trên
        for (y in 0 until height) {
            var foundContent = false
            for (x in 0 until width) {
                val pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                top = y
                break
            }
        }

        // Tìm lề dưới
        for (y in height - 1 downTo 0) {
            var foundContent = false
            for (x in 0 until width) {
                var pixel = grayBitmap.getPixel(x, y) and 0xFF
                if (pixel < threshold) {
                    foundContent = true
                    break
                }
            }
            if (foundContent) {
                bottom = y
                break
            }
        }

        // Thêm lề nhỏ và đảm bảo vùng hợp lệ
        val margin = 10
        left = (left - margin).coerceAtLeast(0)
        top = (top - margin).coerceAtLeast(0)
        right = (right + margin).coerceAtMost(width)
        bottom = (bottom + margin).coerceAtMost(height)

        grayBitmap.recycle()

        return if (right > left && bottom > top) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
            Log.d("OcrRepositoryImpl", "Edge-based cropping: left=$left, top=$top, width=${right - left}, height=${bottom - top}")
            croppedBitmap
        } else {
            Log.w("OcrRepositoryImpl", "Edge detection failed, returning original bitmap")
            bitmap
        }
    }

    private fun convertToArgb8888(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.isHardwareAccelerated()) {
            return bitmap
        }
        val argbBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(argbBitmap)
        try {
            val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }
            canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("OcrRepositoryImpl", "Error converting bitmap to ARGB_8888: ${e.message}")
        }
        return argbBitmap
    }

    private fun Bitmap.isHardwareAccelerated(): Boolean {
        return this.config == Bitmap.Config.HARDWARE
    }

    private suspend fun detectAndCropFace(inputImage: InputImage, bitmap: Bitmap): String? {
        val faces = ocrDataSource.detectFaces(inputImage)
        Log.d("OcrRepositoryImpl", "Number of faces detected: ${faces.size}")
        return if (faces.isNotEmpty()) {
            val faceBitmap = ocrDataSource.cropFaceBitmap(bitmap, faces[0])
            val file = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.png")
            faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
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
                    placeOfOrigin = extractValue(line, listOf("que quan", "quê quán", "place of origin", "place of oigin", "/i", "/", "|", "i", "place of onigin"), lines.getOrNull(i + 1))
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
        val parts = cleanedLine.split(Regex("[:\\s]{2,}"))
        return if (parts.size >= 2) parts[1].trim() else null
    }

    private fun extractValue(line: String, keywords: List<String>, nextLine: String? = null): String? {
        var value = line
        keywords.forEach { keyword ->
            value = value.replace(keyword, "", ignoreCase = true).trim()
        }
        return if (value.isNotBlank() && value.length > 2 && !keywords.any { value.lowercase().contains(it) }) {
            value
        } else {
            nextLine?.takeIf { it.isNotBlank() && it.length > 2 }
        }
    }

    private fun extractDate(text: String): String? {
        val normalizedText = text.replace("o", "0", ignoreCase = true)
        val datePattern = Regex("\\b(\\d{1,2})[\\s/|.-]*(\\d{1,2})[\\s/|.-]*(\\d{4})\\b")
        return datePattern.find(normalizedText)?.let {
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

    private fun getNextValidLine(lines: List< String>, startIndex: Int, excludeKeywords: List<String> = emptyList()): String? {
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