package com.itis.ocrapp.utils

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object ImageProcessingUtils {

    init {
        // Khởi tạo OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("ImageProcessingUtils", "OpenCV initialization failed")
        } else {
            Log.d("ImageProcessingUtils", "OpenCV initialized successfully")
        }
    }

    fun enhanceImageQuality(inputBitmap: Bitmap): Bitmap {
        // Chuyển Bitmap sang Mat
        val mat = Mat()
        Utils.bitmapToMat(inputBitmap, mat)

        // Kiểm tra chất lượng ảnh
        val isSharp = checkSharpness(mat)
        val brightness = checkBrightness(mat)

        var enhancedMat = mat.clone()

        // Nếu ảnh không đủ nét, áp dụng bộ lọc làm sắc nét
        if (!isSharp) {
            enhancedMat = applySharpenFilter(enhancedMat)
        }

        // Điều chỉnh độ sáng và tương phản nếu cần
        if (brightness < 50.0 || brightness > 200.0) {
            enhancedMat = adjustBrightnessContrast(enhancedMat, brightness)
        }

        // Chuyển Mat về Bitmap
        val outputBitmap = Bitmap.createBitmap(enhancedMat.cols(), enhancedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(enhancedMat, outputBitmap)

        // Giải phóng tài nguyên
        mat.release()
        enhancedMat.release()

        return outputBitmap
    }

    private fun checkSharpness(mat: Mat): Boolean {
        // Chuyển sang ảnh xám để tính Laplacian
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Tính Laplacian
        val laplacianMat = Mat()
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F)

        // Tính giá trị trung bình và độ lệch chuẩn
        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacianMat, mean, stddev)

        // Truy cập giá trị stddev
        val stddevArray = stddev.toArray()
        val variance = if (stddevArray.isNotEmpty()) stddevArray[0] * stddevArray[0] else 0.0

        // Ngưỡng: variance < 100 cho thấy ảnh mờ
        Log.d("ImageProcessingUtils", "Laplacian variance: $variance")

        // Giải phóng tài nguyên
        grayMat.release()
        laplacianMat.release()
        mean.release()
        stddev.release()

        return variance >= 100.0
    }

    private fun checkBrightness(mat: Mat): Double {
        // Chuyển sang ảnh xám để tính độ sáng
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Tính giá trị trung bình độ sáng
        val mean = Core.mean(grayMat).`val`[0]
        Log.d("ImageProcessingUtils", "Brightness mean: $mean")
        grayMat.release()

        return mean
    }

    private fun applySharpenFilter(mat: Mat): Mat {
        // Áp dụng bộ lọc làm sắc nét nhẹ nhàng hơn
        val kernel = Mat(3, 3, CvType.CV_32F)
        val kernelData = floatArrayOf(
            0f, -0.5f, 0f,
            -0.5f, 4f, -0.5f,
            0f, -0.5f, 0f
        )
        kernel.put(0, 0, kernelData)

        val sharpenedMat = Mat()
        Imgproc.filter2D(mat, sharpenedMat, -1, kernel)

        kernel.release()
        return sharpenedMat
    }

    private fun adjustBrightnessContrast(mat: Mat, brightness: Double): Mat {
        val adjustedMat = Mat()
        val alpha = 1.1 // Tăng nhẹ độ tương phản (giảm từ 1.2 xuống 1.1)
        val beta = if (brightness < 50.0) 10.0 else if (brightness > 200.0) -10.0 else 0.0 // Điều chỉnh độ sáng nhẹ hơn

        // Áp dụng công thức: new_pixel = alpha * pixel + beta
        Core.convertScaleAbs(mat, adjustedMat, alpha, beta)
        return adjustedMat
    }
}