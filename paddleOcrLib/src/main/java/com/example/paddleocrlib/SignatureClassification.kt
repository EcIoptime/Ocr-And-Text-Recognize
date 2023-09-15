package com.example.paddleocrlib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.util.Size
import java.io.Closeable
import java.util.PriorityQueue
import kotlin.math.min
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.util.Arrays


/** Helper class used to communicate between our app and the TF image classification model */
class SignatureClassification(context: Context, private val maxResult: Int, private val useGpu: Boolean) : Closeable {
    init {
//        MODEL_PATH = copyAssetToStorage(context , "face_model_quant.tflite" )
//        Log.i("test","MODEL_PATH   ${MODEL_PATH}")
    }

    /** Abstraction object that wraps a classification output in an easy to parse way */
//    data class Recognition(val id: String, val title: String, val confidence: Float)
    data class Recognition(
        val id: String,
        val title: String,
        val confidence: Float,
        val rect: RectF,
        var croppedBitmap: Bitmap? = null
    )

    private val preprocessNormalizeOp = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    private val postprocessNormalizeOp = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)
    private val labels by lazy {  arrayListOf("signature")/*FileUtil.loadLabels(context, LABELS_PATH)*/ }
    private var tfInputBuffer = TensorImage(DataType.UINT8)
    private var tfImageProcessor: ImageProcessor? = null

    // Processor to apply post processing of the output probability
    private val probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()

    // Use TFLite in Play Services runtime by setting the option to FROM_SYSTEM_ONLY
    private val interpreterInitializer = lazy {
        val interpreterOption = InterpreterApi.Options()
            .setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)

        if (useGpu) {
            interpreterOption.addDelegateFactory(GpuDelegateFactory())
        }

        Log.i("test" ,"MODEL_PATH  ${MODEL_PATH}")


        InterpreterApi.create(FileUtil.loadMappedFile(context, MODEL_PATH), interpreterOption)

    }

    // Only use interpreter after initialization finished in CameraActivity
    private val interpreter: InterpreterApi by interpreterInitializer
    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = interpreter.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    // Output probability TensorBuffer
//    private val outputProbabilityBuffer: TensorBuffer by lazy {
//        val probabilityTensorIndex = 0
//        val probabilityShape = interpreter.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
//        val probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType()
//        TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
//    }

    // Output probability TensorBuffer
    private val outputProbabilityBuffer: TensorBuffer by lazy {
        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape() // {1, NUM_CLASSES, 4} for coordinates and {1, NUM_CLASSES} for probabilities
        val outputDataType = outputTensor.dataType()
        TensorBuffer.createFixedSize(outputShape, outputDataType)
    }



    /** Classifies the input bitmapBuffer. */
    fun classify(bitmapBuffer: Bitmap, imageRotationDegrees: Int): List<Recognition> {
        // Loads the input bitmapBuffer
        tfInputBuffer = loadImage(bitmapBuffer, imageRotationDegrees)
        Log.i("hello", "tensorSize: ${tfInputBuffer.width} x ${tfInputBuffer.height}")

        // Runs the inference call
        interpreter.run(tfInputBuffer.buffer , outputProbabilityBuffer.buffer.rewind())


        // Get class probability and bounding box coordinates for the max confidence class
        val maxConfidenceIndex = findMaxConfidenceIndex(outputProbabilityBuffer, labels.size)
        if (maxConfidenceIndex == -1) {
            return emptyList()
        }

        val classProbabilities = outputProbabilityBuffer.getFloatArray()
        val boundingBox = getBoundingBox(outputProbabilityBuffer, maxConfidenceIndex)

        val confidence = classProbabilities[maxConfidenceIndex]
        val rect = RectF(
            boundingBox[0],  // Left
            boundingBox[1],  // Top
            boundingBox[2],  // Right
            boundingBox[3]   // Bottom
        )

        // Crop the object from the original image
        val croppedBitmap = cropObjectFromImage(bitmapBuffer, rect)

        return listOf(Recognition("0", labels[maxConfidenceIndex], confidence, rect, croppedBitmap))
    }

    private fun findMaxConfidenceIndex(tensorBuffer: TensorBuffer, numClasses: Int): Int {
        val classProbabilities = tensorBuffer.getFloatArray()
        if (classProbabilities.size != numClasses) {
            return -1
        }

        var maxIndex = 0
        var maxConfidence = classProbabilities[0]

        for (i in 1 until numClasses) {
            if (classProbabilities[i] > maxConfidence) {
                maxIndex = i
                maxConfidence = classProbabilities[i]
            }
        }
        return maxIndex
    }

    private fun getBoundingBox(tensorBuffer: TensorBuffer, classIndex: Int): FloatArray {
        val startIndex = classIndex * 4
        return floatArrayOf(
            tensorBuffer.getFloatValue(startIndex),
            tensorBuffer.getFloatValue(startIndex + 1),
            tensorBuffer.getFloatValue(startIndex + 2),
            tensorBuffer.getFloatValue(startIndex + 3)
        )
    }

    private fun cropObjectFromImage(originalImage: Bitmap, boundingBox: RectF): Bitmap? {
        val left = (boundingBox.left * originalImage.width).toInt()
        val top = (boundingBox.top * originalImage.height).toInt()
        val right = (boundingBox.right * originalImage.width).toInt()
        val bottom = (boundingBox.bottom * originalImage.height).toInt()

        val width = right - left
        val height = bottom - top

        Log.d("hello", "originalImage: width ${originalImage.width} height ${originalImage.height}")
        Log.d("hello", "Left: $left, Top: $top, Right: $right, Bottom: $bottom, Width: $width, Height: $height")


        // Check if the crop region has a valid height
        if (width > 0 && height > 0) {
            return Bitmap.createBitmap(originalImage, left, top, width, height)
        } else {
            return null // Return null to indicate an invalid crop
        }
    }



    /** Releases TFLite resources if initialized. */
    override fun close() {
        if (interpreterInitializer.isInitialized()) {
            interpreter.close()
        }
    }

    /** Loads input image, and applies preprocessing. */
    private fun loadImage(bitmapBuffer: Bitmap, imageRotationDegrees: Int): TensorImage {
        // Initializes preprocessor if null
        return (tfImageProcessor
            ?: run {
                val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
                ImageProcessor.Builder()
                    .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                    .add(ResizeOp(
                        tfInputSize.height,
                        tfInputSize.width,
                        ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                    ))
                    .add(Rot90Op(-imageRotationDegrees / 90))
                    .add(preprocessNormalizeOp)
                    .build()
                    .also {
                        tfImageProcessor = it
                        Log.d(TAG, "tfImageProcessor initialized successfully. imageSize: $cropSize")
                    }
            })
            .process(tfInputBuffer.apply { load(bitmapBuffer) })
    }

    /** Gets the top-k results. */
//    private fun getTopKProbability(labelProb: Map<String, Float>): List<Recognition> {
//        // Sort the recognition by confidence from high to low.
//        val pq: PriorityQueue<Recognition> = PriorityQueue(maxResult, compareByDescending<Recognition> { it.confidence })
//        pq += labelProb.map { (label, prob) -> Recognition(label, label, prob) }
//        return List(min(maxResult, pq.size)) { pq.poll()!! }
//    }

    private fun copyAssetToStorage(context: Context, assetFileName: String): String {
        val outputDir = context.filesDir // You can change this to another directory if needed
        val outputFile = File(outputDir, assetFileName)

        // Copy the asset to the destination file
        context.assets.open(assetFileName).use { inputStream ->
            outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return outputFile.absolutePath
    }

    val MODEL_PATH by lazy { "signature.tflite"   } //"face_model_quant.tflite"
    companion object {
        private val TAG = SignatureClassification::class.java.simpleName

        // ClassifierFloatEfficientNet model

//        private const val LABELS_PATH = "labels_without_background.txt"

        // Float model does not need dequantization in the post-processing. Setting mean and std as
        // 0.0f and 1.0f, respectively, to bypass the normalization
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 1.0f
        private const val IMAGE_MEAN = 127.0f
        private const val IMAGE_STD = 128.0f
    }
}