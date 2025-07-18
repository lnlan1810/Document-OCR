package com.itis.ocrapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.itis.ocrapp.data.source.OcrDataSource
import com.itis.ocrapp.utils.EncryptionUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FaceDetector(
    private val ocrDataSource: OcrDataSource,
    private val context: Context
) {
    private val TAG = "FaceDetector"

    suspend fun detectAndCropFace(inputImage: InputImage, bitmap: Bitmap): String? {
        val faces = ocrDataSource.detectFaces(inputImage)
        android.util.Log.d(TAG, "Number of faces detected: ${faces.size}")
        return if (faces.isNotEmpty()) {
            val faceBitmap = ocrDataSource.cropFaceBitmap(bitmap, faces[0])
            val file = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.png")
            faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
            val encFaceFile = File(context.cacheDir, "face_image_${System.currentTimeMillis()}.enc")
            val tempFaceFile = File(context.cacheDir, "temp_face_image.png")
            FileInputStream(file).use { input ->
                FileOutputStream(tempFaceFile).use { output -> input.copyTo(output) }
            }
            EncryptionUtils.encryptFile(context, tempFaceFile, encFaceFile)
            tempFaceFile.delete()
            file.delete()
            encFaceFile.absolutePath
        } else {
            null
        }
    }
}