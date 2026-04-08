package com.example.imagepreprocessingtest

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagepreprocessingtest.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var preprocessor: ImagePreprocessor

    private var currentImageIndex = 0
    private var currentBitmap: Bitmap? = null
    private val imageFiles = mutableListOf<String>()
    private var isOpenCVInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // OpenCV初期化
        initOpenCV()

        // 画像リストの読み込み
        loadImageList()

        // 最初の画像を読み込み
        if (imageFiles.isNotEmpty()) {
            loadCurrentImage()
        }

        setupButtons()
        setupSpinner()
    }

    private fun initOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
            isOpenCVInitialized = true
            preprocessor = ImagePreprocessor()
            binding.textViewStatus.text = "OpenCV: ✅ 初期化完了"
        } else {
            Log.e(TAG, "OpenCV initialization failed")
            isOpenCVInitialized = false
            binding.textViewStatus.text = "OpenCV: ❌ 初期化失敗"
            Toast.makeText(this, "OpenCVの初期化に失敗しました", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        binding.buttonProcess.setOnClickListener {
            if (!isOpenCVInitialized) {
                Toast.makeText(this, "OpenCVが初期化されていません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processCurrentImage()
        }

        binding.buttonProcessDetailed.setOnClickListener {
            if (!isOpenCVInitialized) {
                Toast.makeText(this, "OpenCVが初期化されていません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processCurrentImageDetailed()
        }

        binding.buttonNext.setOnClickListener {
            if (currentImageIndex < imageFiles.size - 1) {
                currentImageIndex++
                binding.spinnerImages.setSelection(currentImageIndex)
                loadCurrentImage()
            }
        }

        binding.buttonPrev.setOnClickListener {
            if (currentImageIndex > 0) {
                currentImageIndex--
                binding.spinnerImages.setSelection(currentImageIndex)
                loadCurrentImage()
            }
        }
    }

    private fun setupSpinner() {
        binding.spinnerImages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (currentImageIndex != position) {
                    currentImageIndex = position
                    loadCurrentImage()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadImageList() {
        try {
            val assets = assets.list("images")
            if (assets != null) {
                imageFiles.clear()
                imageFiles.addAll(assets.filter {
                    it.endsWith(".jpg", ignoreCase = true) ||
                    it.endsWith(".jpeg", ignoreCase = true) ||
                    it.endsWith(".png", ignoreCase = true)
                }.sorted())

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, imageFiles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerImages.adapter = adapter

                binding.textViewImageCount.text = "画像数: ${imageFiles.size}枚"
                Log.i(TAG, "Loaded ${imageFiles.size} images")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image list", e)
            Toast.makeText(this, "画像リストの読み込みに失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentImage() {
        if (imageFiles.isEmpty()) return

        val filename = imageFiles[currentImageIndex]
        binding.textViewImageName.text = "画像: $filename"

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = assets.open("images/$filename")
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load image: $filename", e)
                    null
                }
            }

            if (bitmap != null) {
                currentBitmap = bitmap
                binding.imageViewOriginal.setImageBitmap(bitmap)
                binding.imageViewProcessed.setImageBitmap(null)
                binding.textViewProcessingTime.text = "処理時間: -"
                binding.textViewImageSize.text = "サイズ: ${bitmap.width} x ${bitmap.height}"
                binding.textViewDetailedTiming.text = ""
            } else {
                Toast.makeText(this@MainActivity, "画像の読み込みに失敗: $filename", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processCurrentImage() {
        val bitmap = currentBitmap ?: return

        CoroutineScope(Dispatchers.Main).launch {
            setProcessingState(true)

            val (processedBitmap, processingTime) = withContext(Dispatchers.Default) {
                preprocessor.preprocessWithTiming(bitmap)
            }

            binding.imageViewProcessed.setImageBitmap(processedBitmap)
            binding.textViewProcessingTime.text = "処理時間: ${processingTime}ms"
            binding.textViewDetailedTiming.text = ""
            
            setProcessingState(false)
            showResultToast(processingTime)
        }
    }

    private fun processCurrentImageDetailed() {
        val bitmap = currentBitmap ?: return

        CoroutineScope(Dispatchers.Main).launch {
            setProcessingState(true)

            val result = withContext(Dispatchers.Default) {
                preprocessor.preprocessWithDetailedTiming(bitmap)
            }

            binding.imageViewProcessed.setImageBitmap(result.bitmap)
            binding.textViewProcessingTime.text = "合計: ${result.totalTimeMs}ms"
            
            // 詳細タイミング表示
            val detailText = result.stepTimings.entries.joinToString("\n") { (step, time) ->
                "  $step: ${time}ms"
            }
            binding.textViewDetailedTiming.text = detailText

            setProcessingState(false)
            showResultToast(result.totalTimeMs)
        }
    }

    private fun setProcessingState(isProcessing: Boolean) {
        binding.buttonProcess.isEnabled = !isProcessing
        binding.buttonProcessDetailed.isEnabled = !isProcessing
        binding.buttonProcess.text = if (isProcessing) "処理中..." else "前処理実行"
        binding.buttonProcessDetailed.text = if (isProcessing) "処理中..." else "詳細計測"
    }

    private fun showResultToast(processingTime: Long) {
        val message = if (processingTime <= 3000) {
            "✅ 目標達成: ${processingTime}ms (< 3秒)"
        } else {
            "⚠️ 目標超過: ${processingTime}ms (> 3秒)"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
