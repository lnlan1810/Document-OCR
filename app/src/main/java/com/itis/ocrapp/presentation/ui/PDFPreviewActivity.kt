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
import androidx.lifecycle.lifecycleScope
import com.itis.ocrapp.databinding.ActivityPdfpreviewBinding
import com.itis.ocrapp.utils.EncryptionUtils
import com.itis.ocrapp.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // Song song hóa giải mã hình ảnh và hiển thị văn bản
        lifecycleScope.launch {
            val imageLoadingDeferred = async(Dispatchers.IO) {
                documentImagePath?.let {
                    try {
                        val encFile = File(it)
                        val tempFile = File(cacheDir, "temp_document_image.png")
                        EncryptionUtils.decryptFile(this@PDFPreviewActivity, encFile, tempFile)
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        tempFile.delete()
                        // Chia tỷ lệ để phù hợp với A4 ở 150 DPI, giữ chất lượng
                        bitmap?.let { scaleBitmap(it, 1240, 1754) }
                    } catch (e: Exception) {
                        runOnUiThread { showToast("Không thể tải hình ảnh tài liệu") }
                        null
                    }
                }
            }

            val textLoadingDeferred = async(Dispatchers.Main) {
                translationText?.let {
                    binding.etTranslation.setText(it)
                } ?: binding.etTranslation.setText("Không có văn bản dịch")
            }

            // Chờ cả hai tác vụ hoàn thành
            documentBitmap = imageLoadingDeferred.await()
            textLoadingDeferred.await()

            // Hiển thị hình ảnh nếu có
            documentBitmap?.let { bitmap ->
                binding.imgDocument.setImageBitmap(bitmap)
            } ?: showToast("Không có hình ảnh tài liệu")
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


    private suspend fun createOriginalPdf(originalImage: Bitmap, outputFile: File) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 1240 // A4 ở 150 DPI
        val pageHeight = 1754
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        // Căn chỉnh bitmap để lấp đầy trang, giữ tỷ lệ
        val bitmapAspect = originalImage.width.toFloat() / originalImage.height
        val pageAspect = pageWidth.toFloat() / pageHeight
        val scale: Float
        val dx: Float
        val dy: Float
        if (bitmapAspect > pageAspect) {
            scale = pageWidth.toFloat() / originalImage.width
            dx = 0f
            dy = (pageHeight - originalImage.height * scale) / 2
        } else {
            scale = pageHeight.toFloat() / originalImage.height
            dx = (pageWidth - originalImage.width * scale) / 2
            dy = 0f
        }
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(originalImage, 0f, 0f, Paint().apply { isAntiAlias = true })
        document.finishPage(page)
        document.writeTo(FileOutputStream(outputFile))
        document.close()
    }

    private suspend fun createTranslationPdf(translationText: String, outputFile: File) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 1240 // A4 ở 150 DPI
        val pageHeight = 1754
        val margin = 50f
        val lineSpacing = 25f
        var y = 50f
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
        }

        var currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        var canvas = currentPage.canvas
        var pageNumber = 1

        // Tách văn bản thành các đoạn dựa trên ký tự xuống dòng
        val paragraphs = translationText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (paragraph in paragraphs) {
            // Tách đoạn thành các dòng phù hợp với chiều rộng trang
            val lines = splitTextToFit(paragraph, paint, pageWidth - 2 * margin)
            for (line in lines) {
                if (y + lineSpacing > pageHeight - margin) {
                    document.finishPage(currentPage)
                    pageNumber++
                    currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = currentPage.canvas
                    y = 50f
                }
                canvas.drawText(line, margin, y, paint)
                y += lineSpacing
            }
            // Thêm khoảng cách giữa các đoạn
            y += lineSpacing * 0.5f
        }

        document.finishPage(currentPage)
        document.writeTo(FileOutputStream(outputFile))
        document.close()
    }

    private fun splitTextToFit(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
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
            showToast("Không có hình ảnh tài liệu")
            return
        }
        lifecycleScope.launch {
            try {
                val pdfDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OCRApp_PDFs")
                if (!pdfDir.exists()) pdfDir.mkdirs()
                val originalPdfFile = File(pdfDir, "original_${System.currentTimeMillis()}.pdf")
                createOriginalPdf(documentBitmap!!, originalPdfFile)
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@PDFPreviewActivity, PDFViewerActivity::class.java).apply {
                        putExtra("PDF_FILE_PATH", originalPdfFile.absolutePath)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Lỗi khi tạo PDF gốc: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    private fun generateAndViewTranslationPDF() {
        if (binding.etTranslation.text.isEmpty()) {
            showToast("Không có văn bản dịch")
            return
        }
        lifecycleScope.launch {
            try {
                val pdfDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OCRApp_PDFs")
                if (!pdfDir.exists()) pdfDir.mkdirs()
                val translationPdfFile = File(pdfDir, "translation_${System.currentTimeMillis()}.pdf")
                createTranslationPdf(binding.etTranslation.text.toString(), translationPdfFile)
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@PDFPreviewActivity, PDFViewerActivity::class.java).apply {
                        putExtra("PDF_FILE_PATH", translationPdfFile.absolutePath)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Lỗi khi tạo PDF dịch: ${e.message}")
                }
                e.printStackTrace()
            }
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
            showToast("Đã cấp quyền truy cập bộ nhớ, vui lòng thử lại")
        } else {
            showToast("Quyền truy cập bộ nhớ bị từ chối")
        }
    }
}