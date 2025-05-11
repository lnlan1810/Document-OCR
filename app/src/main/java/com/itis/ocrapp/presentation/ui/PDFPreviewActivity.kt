package com.itis.ocrapp.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.itis.ocrapp.databinding.ActivityPdfpreviewBinding
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.showToast
import java.io.File
import java.io.FileOutputStream

class PDFPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfpreviewBinding
    private var documentBitmap: Bitmap? = null
    private var translationText: String? = null
    private var documentImagePath: String? = null
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfpreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        documentImagePath = intent.getStringExtra("DOCUMENT_IMAGE_PATH")
        translationText = intent.getStringExtra("TRANSLATION_TEXT")

        // Giải mã và hiển thị hình ảnh tài liệu
        documentImagePath?.let {
            val encFile = File(it)
            val tempFile = File(cacheDir, "temp_document_image.png")
            EncryptionUtils.decryptFile(this, encFile, tempFile)
            val file = File(tempFile.absolutePath)
            if (file.exists()) {
                documentBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                tempFile.delete() // Xóa tệp tạm sau khi sử dụng
                documentBitmap?.let { bitmap ->
                    binding.imgDocument.setImageBitmap(bitmap)
                } ?: showToast("Не удалось загрузить изображение документа\"")
            } else {
                showToast("Файл изображения документа не найден")
            }
        } ?: showToast("Изображение документа не предоставлено")

        translationText?.let {
            binding.etTranslation.setText(it)
        } ?: binding.etTranslation.setText("Переведённый текст не предоставлен")

        binding.btnPreview.setOnClickListener {
            previewPDFLayout()
        }
        binding.btnGenerateOriginalPdf.setOnClickListener {
            if (checkStoragePermission()) {
                generateAndViewOriginalPDF()
            } else {
                requestStoragePermission()
            }
        }
        binding.btnGenerateTranslationPdf.setOnClickListener {
            if (checkStoragePermission()) {
                generateAndViewTranslationPDF()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun previewPDFLayout() {
        translationText = binding.etTranslation.text.toString()
        if (translationText.isNullOrEmpty()) {
            showToast("Переведённый текст пуст")
            return
        }
        showToast("Предварительный просмотр обновлён с отредактированным текстом: $translationText")
    }

    private fun createOriginalPdf(originalImage: Bitmap, outputFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val scaledBitmap = scaleBitmap(originalImage, pageWidth, pageHeight)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        document.finishPage(page)
        document.writeTo(FileOutputStream(outputFile))
        document.close()
        scaledBitmap.recycle()
    }

    private fun createTranslationPdf(translationText: String, outputFile: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
        }
        val lines = translationText.split("\n")
        var y = 50f
        val margin = 50f
        val lineSpacing = 30f
        for (line in lines) {
            if (y + lineSpacing > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                val newPage = document.startPage(pageInfo)
                y = 50f
                newPage.canvas.drawText(line, margin, y, paint)
                y += lineSpacing
            } else {
                canvas.drawText(line, margin, y, paint)
                y += lineSpacing
            }
        }
        document.finishPage(page)
        document.writeTo(FileOutputStream(outputFile))
        document.close()
    }

    private fun scaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        val newWidth: Int
        val newHeight: Int
        if (aspectRatio > targetWidth.toFloat() / targetHeight) {
            newWidth = targetWidth
            newHeight = (targetWidth / aspectRatio).toInt()
        } else {
            newHeight = targetHeight
            newWidth = (targetHeight * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun generateAndViewOriginalPDF() {
        if (documentBitmap == null) {
            showToast("Отсутствует изображение документа")
            return
        }
        try {
            val pdfDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OCRApp_PDFs")
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val originalPdfFile = File(pdfDir, "original_${System.currentTimeMillis()}.pdf")
            createOriginalPdf(documentBitmap!!, originalPdfFile)
            val intent = Intent(this, PDFViewerActivity::class.java).apply {
                putExtra("PDF_FILE_PATH", originalPdfFile.absolutePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Ошибка при создании оригинального PDF: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun generateAndViewTranslationPDF() {
        if (binding.etTranslation.text.isEmpty()) {
            showToast("Отсутствует переведённый текст")
            return
        }
        try {
            val pdfDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OCRApp_PDFs")
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val translationPdfFile = File(pdfDir, "translation_${System.currentTimeMillis()}.pdf")
            createTranslationPdf(binding.etTranslation.text.toString(), translationPdfFile)
            val intent = Intent(this, PDFViewerActivity::class.java).apply {
                putExtra("PDF_FILE_PATH", translationPdfFile.absolutePath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Ошибка при создании переведённого PDF: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showToast("Доступ к хранилищу разрешён, пожалуйста, попробуйте снова")
        } else {
            showToast("Доступ к хранилищу отклонён")
        }
    }
}