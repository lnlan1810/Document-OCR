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
                // Giảm kích thước bitmap để tăng tốc render
                val maxWidth = (displayMetrics.widthPixels * 0.8).toInt()
                val maxHeight = (displayMetrics.heightPixels * 0.5).toInt()
                val scale = Math.min(maxWidth.toFloat() / page.width, maxHeight.toFloat() / page.height)
                val bitmapWidth = (page.width * scale).toInt()
                val bitmapHeight = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                binding.pdfImageView.setImageBitmap(bitmap)
                page.close()
            } else {
                showToast("В PDF нет страниц")
            }
        } catch (e: Exception) {
            showToast("Ошибка отображения PDF: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun downloadPDF() {
        pdfFilePath?.let { path ->
            try {
                val sourceFile = File(path)
                if (!sourceFile.exists()) {
                    showToast("PDF-файл не существует")
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
                            showToast("PDF загружен в папку «Загрузки»: ${sourceFile.name}")
                        } else {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                NOTIFICATION_PERMISSION_CODE
                            )
                        }
                    } else {
                        showDownloadNotification(sourceFile.name, uri)
                        showToast("PDF загружен в папку «Загрузки»: ${sourceFile.name}")
                    }
                } ?: showToast("Ошибка сохранения PDF")
            } catch (e: Exception) {
                showToast("Ошибка загрузки PDF: ${e.message}")
                e.printStackTrace()
            }
        } ?: showToast("Нет PDF-файла, доступного для загрузки")
    }

    private fun showDownloadNotification(fileName: String, fileUri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (openIntent.resolveActivity(packageManager) == null) {
            showToast("Просмотрщик PDF не найден")
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
            .setContentTitle("PDF-файл загружен")
            .setContentText("File $fileName сохранён в разделе «Загрузки». Нажмите, чтобы открыть.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            with(NotificationManagerCompat.from(this)) {
                notify(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            showToast("Невозможно отобразить уведомление из-за отсутствия разрешений")
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
                        showToast("PDF загружен: $name")
                    }
                }
            } else {
                showToast("Разрешение на уведомление отклонено, уведомление не может быть отображено")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentRenderer?.close()
        currentFileDescriptor?.close()
    }
}