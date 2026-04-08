package com.example.imagepreprocessingtest

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.system.measureTimeMillis

/**
 * 画像前処理クラス（OpenCV版）
 * 鉄骨の手書き文字認識のための前処理を行う
 */
class ImagePreprocessor {

    companion object {
        private const val TAG = "ImagePreprocessor"
        
        // デフォルトパラメータ
        const val DEFAULT_TARGET_SIZE = 960
        const val DEFAULT_CLAHE_CLIP_LIMIT = 2.0
        const val DEFAULT_CLAHE_TILE_SIZE = 8
        const val DEFAULT_GAUSSIAN_KERNEL_SIZE = 5
        const val DEFAULT_ADAPTIVE_BLOCK_SIZE = 11
        const val DEFAULT_ADAPTIVE_C = 2.0
    }

    /**
     * 画像前処理のメイン関数
     * @param bitmap 入力画像
     * @return 前処理済み画像
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        var processedMat = srcMat

        // 1. リサイズ
        processedMat = resize(processedMat, DEFAULT_TARGET_SIZE, DEFAULT_TARGET_SIZE)

        // 2. グレースケール化
        processedMat = toGrayscale(processedMat)

        // 3. CLAHE（コントラスト補正）
        processedMat = applyCLAHE(processedMat, DEFAULT_CLAHE_CLIP_LIMIT, DEFAULT_CLAHE_TILE_SIZE)

        // 4. ノイズ除去（ガウシアンブラー）
        processedMat = denoise(processedMat, DEFAULT_GAUSSIAN_KERNEL_SIZE)

        // 5. 適応二値化
        processedMat = adaptiveThreshold(processedMat, DEFAULT_ADAPTIVE_BLOCK_SIZE, DEFAULT_ADAPTIVE_C)

        // Matをグレースケールから3チャンネルに変換（表示用）
        val displayMat = Mat()
        Imgproc.cvtColor(processedMat, displayMat, Imgproc.COLOR_GRAY2RGBA)

        val resultBitmap = Bitmap.createBitmap(
            displayMat.cols(),
            displayMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(displayMat, resultBitmap)

        // メモリ解放
        srcMat.release()
        processedMat.release()
        displayMat.release()

        return resultBitmap
    }

    /**
     * 処理時間を測定しながら前処理を実行
     */
    fun preprocessWithTiming(bitmap: Bitmap): Pair<Bitmap, Long> {
        var processedBitmap: Bitmap? = null
        val processingTime = measureTimeMillis {
            processedBitmap = preprocess(bitmap)
        }
        return Pair(processedBitmap!!, processingTime)
    }

    /**
     * 各ステップの処理時間を詳細に測定
     */
    fun preprocessWithDetailedTiming(bitmap: Bitmap): PreprocessResult {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        var processedMat = srcMat
        val timings = mutableMapOf<String, Long>()

        // 1. リサイズ
        val resizeTime = measureTimeMillis {
            processedMat = resize(processedMat, DEFAULT_TARGET_SIZE, DEFAULT_TARGET_SIZE)
        }
        timings["resize"] = resizeTime

        // 2. グレースケール化
        val grayscaleTime = measureTimeMillis {
            processedMat = toGrayscale(processedMat)
        }
        timings["grayscale"] = grayscaleTime

        // 3. CLAHE
        val claheTime = measureTimeMillis {
            processedMat = applyCLAHE(processedMat, DEFAULT_CLAHE_CLIP_LIMIT, DEFAULT_CLAHE_TILE_SIZE)
        }
        timings["clahe"] = claheTime

        // 4. ノイズ除去
        val denoiseTime = measureTimeMillis {
            processedMat = denoise(processedMat, DEFAULT_GAUSSIAN_KERNEL_SIZE)
        }
        timings["denoise"] = denoiseTime

        // 5. 適応二値化
        val thresholdTime = measureTimeMillis {
            processedMat = adaptiveThreshold(processedMat, DEFAULT_ADAPTIVE_BLOCK_SIZE, DEFAULT_ADAPTIVE_C)
        }
        timings["threshold"] = thresholdTime

        // 表示用に変換
        val displayMat = Mat()
        Imgproc.cvtColor(processedMat, displayMat, Imgproc.COLOR_GRAY2RGBA)

        val resultBitmap = Bitmap.createBitmap(
            displayMat.cols(),
            displayMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(displayMat, resultBitmap)

        val totalTime = timings.values.sum()

        // メモリ解放
        srcMat.release()
        processedMat.release()
        displayMat.release()

        return PreprocessResult(resultBitmap, totalTime, timings)
    }

    /**
     * リサイズ（アスペクト比を維持）
     */
    private fun resize(mat: Mat, maxWidth: Int, maxHeight: Int): Mat {
        val width = mat.cols()
        val height = mat.rows()

        val scaleWidth = maxWidth.toDouble() / width
        val scaleHeight = maxHeight.toDouble() / height
        val scale = minOf(scaleWidth, scaleHeight)

        if (scale >= 1.0) return mat

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = Mat()
        Imgproc.resize(mat, resized, Size(newWidth.toDouble(), newHeight.toDouble()))
        mat.release()
        return resized
    }

    /**
     * グレースケール化
     */
    private fun toGrayscale(mat: Mat): Mat {
        val gray = Mat()
        when (mat.channels()) {
            1 -> return mat
            3 -> Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
            4 -> Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY)
            else -> return mat
        }
        mat.release()
        return gray
    }

    /**
     * CLAHE（Contrast Limited Adaptive Histogram Equalization）
     * コントラストを改善し、文字を見やすくする
     */
    private fun applyCLAHE(mat: Mat, clipLimit: Double, tileSize: Int): Mat {
        val clahe = Imgproc.createCLAHE(clipLimit, Size(tileSize.toDouble(), tileSize.toDouble()))
        val result = Mat()
        clahe.apply(mat, result)
        mat.release()
        return result
    }

    /**
     * ノイズ除去（ガウシアンブラー）
     */
    private fun denoise(mat: Mat, kernelSize: Int): Mat {
        val denoised = Mat()
        Imgproc.GaussianBlur(mat, denoised, Size(kernelSize.toDouble(), kernelSize.toDouble()), 0.0)
        mat.release()
        return denoised
    }

    /**
     * 適応二値化
     * 局所的な明るさに応じて閾値を決定
     */
    private fun adaptiveThreshold(mat: Mat, blockSize: Int, c: Double): Mat {
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            mat,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            blockSize,
            c
        )
        mat.release()
        return binary
    }

    /**
     * 前処理結果を格納するデータクラス
     */
    data class PreprocessResult(
        val bitmap: Bitmap,
        val totalTimeMs: Long,
        val stepTimings: Map<String, Long>
    )
}




