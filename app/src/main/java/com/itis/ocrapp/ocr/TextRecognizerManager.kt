package com.itis.ocrapp.ocr

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognizerManager(
    private val onSuccess: (String) -> Unit,
    private val onFailure: (String) -> Unit,
    private val onSuccessWithSource: (String, Boolean) -> Unit // Thêm tham số để phân biệt nguồn
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(image: InputImage, isFromGallery: Boolean = false) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccessWithSource(visionText.text, isFromGallery)
            }
            .addOnFailureListener { e ->
                onFailure("Ошибка идентификации: ${e.message}")
            }
    }
}