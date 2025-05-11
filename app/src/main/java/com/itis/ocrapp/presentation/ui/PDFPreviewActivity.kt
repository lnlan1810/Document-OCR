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

        // Lấy dữ liệu từ Intent
        documentImagePath = intent.getStringExtra("DOCUMENT_IMAGE_PATH")
        translationText = intent.getStringExtra("TRANSLATION_TEXT")

        // Hiển thị hình ảnh tài liệu gốc
        documentImagePath?.let {
            val file = File(it)
            if (file.exists()) {
                documentBitmap = BitmapFactory.decodeFile(it)
                documentBitmap?.let { bitmap ->
                    binding.imgDocument.setImageBitmap(bitmap)
                } ?: showToast("Không thể tải hình ảnh tài liệu")
            } else {
                showToast("Không tìm thấy file hình ảnh tài liệu")
            }
        } ?: showToast("Không có hình ảnh tài liệu được cung cấp")

        // Hiển thị văn bản bản dịch
        translationText?.let {
            binding.etTranslation.setText(it)
        } ?: binding.etTranslation.setText("Không có văn bản bản dịch được cung cấp")

        // Xử lý nút xem trước
        binding.btnPreview.setOnClickListener {
            previewPDFLayout()
        }

        // Xử lý nút tạo và xem PDF bản gốc
        binding.btnGenerateOriginalPdf.setOnClickListener {
            if (checkStoragePermission()) {
                generateAndViewOriginalPDF()
            } else {
                requestStoragePermission()
            }
        }

        // Xử lý nút tạo và xem PDF bản dịch
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
            showToast("Văn bản bản dịch trống")
            return
        }
        showToast("Đã cập nhật bản xem trước với văn bản đã chỉnh sửa: $translationText")
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
            showToast("Thiếu hình ảnh tài liệu")
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
            showToast("Lỗi khi tạo PDF bản gốc: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun generateAndViewTranslationPDF() {
        if (binding.etTranslation.text.isEmpty()) {
            showToast("Thiếu văn bản bản dịch")
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
            showToast("Lỗi khi tạo PDF bản dịch: ${e.message}")
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
            showToast("Quyền truy cập bộ nhớ được cấp, vui lòng thử lại")
        } else {
            showToast("Quyền truy cập bộ nhớ bị từ chối")
        }
    }
}