package com.example.paddleocrlib

import android.content.Context
import android.graphics.Bitmap
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


/** Helper class used to communicate between our app and the TF image classification model */
internal class ImageClassificationHelper(context: Context, private val maxResult: Int, private val useGpu: Boolean) : Closeable {
    init {
//        MODEL_PATH = copyAssetToStorage(context , "face_model_quant.tflite" )
//        Log.i("test","MODEL_PATH   ${MODEL_PATH}")
    }

    /** Abstraction object that wraps a classification output in an easy to parse way */
    data class Recognition(val id: String, val title: String, val confidence: Float)

    private val preprocessNormalizeOp = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    private val postprocessNormalizeOp = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)
    private val labels by lazy {  arrayListOf("List 1")/*FileUtil.loadLabels(context, LABELS_PATH)*/ }
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
    private val outputProbabilityBuffer: TensorBuffer by lazy {
        val probabilityTensorIndex = 0
        val probabilityShape =
            interpreter.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
        val probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType()
        TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
    }

    /** Classifies the input bitmapBuffer. */
    fun classify(bitmapBuffer: Bitmap, imageRotationDegrees: Int): FloatArray? {
        // Loads the input bitmapBuffer
        tfInputBuffer = loadImage(bitmapBuffer, imageRotationDegrees)
        Log.d(TAG, "tensorSize: ${tfInputBuffer.width} x ${tfInputBuffer.height}")

        // Runs the inference call
        interpreter.run(tfInputBuffer.buffer, outputProbabilityBuffer.buffer.rewind())


//        val embedding = outputProbabilityBuffer.floatArray?.map { it }?.joinToString { it.toString() }

//        Log.i("test" ,"output float ${outputProbabilityBuffer.floatArray?.size}")
//        Log.i("test" ,"output embedding ${embedding}")
//        Log.i("test" ,"output int ${outputProbabilityBuffer.intArray?.size}")

        // Gets the map of label and probability
//        val labeledProbability = TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer)).mapWithFloatValue

//        return getTopKProbability(labeledProbability)
        return  outputProbabilityBuffer.floatArray
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
    private fun getTopKProbability(labelProb: Map<String, Float>): List<Recognition> {
        // Sort the recognition by confidence from high to low.
        val pq: PriorityQueue<Recognition> =
            PriorityQueue(maxResult, compareByDescending<Recognition> { it.confidence })
        pq += labelProb.map { (label, prob) -> Recognition(label, label, prob) }
        return List(min(maxResult, pq.size)) { pq.poll()!! }
    }

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

    val MODEL_PATH by lazy { "face_model_quant.tflite" /*copyAssetToStorage(context , "face_model_quant.tflite" )*/  } //"face_model_quant.tflite"
    companion object {
        private val TAG = ImageClassificationHelper::class.java.simpleName

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