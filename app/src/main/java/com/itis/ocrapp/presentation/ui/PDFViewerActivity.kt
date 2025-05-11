package com.itis.ocrapp.presentation.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.itis.ocrapp.R
import com.itis.ocrapp.databinding.ActivityPdfviewerBinding
import com.itis.ocrapp.utils.showToast
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream

class PDFViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfviewerBinding
    private var currentRenderer: PdfRenderer? = null
    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var pdfFilePath: String? = null
    private var pendingFileName: String? = null
    private var pendingFileUri: Uri? = null
    private val NOTIFICATION_ID = 1001
    private val NOTIFICATION_PERMISSION_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfviewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        pdfFilePath = intent.getStringExtra("PDF_FILE_PATH")
        pdfFilePath?.let { displayPDF(File(it)) }

        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnDownload.setOnClickListener {
            downloadPDF()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.ocr_download_channel_name)
            val descriptionText = getString(R.string.ocr_download_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ocr_download_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayPDF(pdfFile: File) {
        try {
            currentRenderer?.close()
            currentFileDescriptor?.close()
            currentFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            currentRenderer = PdfRenderer(currentFileDescriptor!!)
            if (currentRenderer!!.pageCount > 0) {
                val page = currentRenderer!!.openPage(0)
                val displayMetrics = resources.displayMetrics
                val maxWidth = displayMetrics.widthPixels
                val maxHeight = (displayMetrics.heightPixels * 0.6).toInt()
                val scale = Math.min(maxWidth.toFloat() / page.width, maxHeight.toFloat() / page.height)
                val bitmapWidth = (page.width * scale).toInt()
                val bitmapHeight = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                binding.pdfImageView.setImageBitmap(bitmap)
                page.close()
            } else {
                showToast("PDF không có trang nào")
            }
        } catch (e: Exception) {
            showToast("Lỗi khi hiển thị PDF: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun downloadPDF() {
        pdfFilePath?.let { path ->
            try {
                val sourceFile = File(path)
                if (!sourceFile.exists()) {
                    showToast("File PDF không tồn tại")
                    return
                }
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it).use { output ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(output!!)
                        }
                    }
                    contentResolver.update(uri, contentValues, null, null)
                    pendingFileName = sourceFile.name
                    pendingFileUri = uri
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            showDownloadNotification(sourceFile.name, uri)
                            showToast("Đã tải PDF về thư mục Downloads: ${sourceFile.name}")
                        } else {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_CODE
                            )
                        }
                    } else {
                        showDownloadNotification(sourceFile.name, uri)
                        showToast("Đã tải PDF về thư mục Downloads: ${sourceFile.name}")
                    }
                } ?: showToast("Lỗi khi lưu PDF")
            } catch (e: Exception) {
                showToast("Lỗi khi tải PDF: ${e.message}")
                e.printStackTrace()
            }
        } ?: showToast("Không có file PDF để tải")
    }

    private fun showDownloadNotification(fileName: String, fileUri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (openIntent.resolveActivity(packageManager) == null) {
            showToast("Không tìm thấy ứng dụng xem PDF")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        val notification = NotificationCompat.Builder(this, "ocr_download_channel")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("PDF Đã Tải")
            .setContentText("File $fileName đã được lưu vào Downloads. Nhấn để mở.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            with(NotificationManagerCompat.from(this)) {
                notify(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            showToast("Không thể hiển thị thông báo do thiếu quyền")
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingFileName?.let { name ->
                    pendingFileUri?.let { uri ->
                        showDownloadNotification(name, uri)
                        showToast("Đã tải PDF về thư mục Downloads: $name")
                    }
                }
            } else {
                showToast("Quyền thông báo bị từ chối, không thể hiển thị thông báo")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentRenderer?.close()
        currentFileDescriptor?.close()
    }
}