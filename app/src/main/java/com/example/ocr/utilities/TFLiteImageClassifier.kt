package com.example.ocr.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
//import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteImageClassifier(private val context: Context) {

//    private val interpreter: Interpreter
//
//    init {
//        val modelFile = loadModelFile()
//        interpreter = Interpreter(modelFile)
//    }

    private fun loadModelFile(): ByteBuffer {
        val modelFileDescriptor = context.assets.openFd("model.tflite")
        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val bufferSize = modelFileDescriptor.length.toInt()
        val modelByteBuffer = ByteBuffer.allocateDirect(bufferSize)
        inputStream.channel.use { channel ->
            channel.read(modelByteBuffer)
        }
        return modelByteBuffer
    }

//    fun classifyImage(bitmap: Bitmap): List<ClassificationResult> {
//        val inputBuffer = preprocessImage(bitmap)
//        val outputBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4) // Assuming 4 bytes per float
//        interpreter.run(inputBuffer, outputBuffer)
//        return postprocessOutput(outputBuffer)
//    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 112, 112, true)
        val inputBuffer = ByteBuffer.allocateDirect(112 * 112 * 3 * 4) // RGB image, 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(112 * 112)
        scaledBitmap.getPixels(pixels, 0, 112, 0, 0, 112, 112)
        for (pixelValue in pixels) {
            inputBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        return inputBuffer
    }

    private fun postprocessOutput(outputBuffer: ByteBuffer): List<ClassificationResult> {
        val results = mutableListOf<ClassificationResult>()
        for (i in 0 until NUM_CLASSES) {
            val confidence = outputBuffer.getFloat(i * 4)
            results.add(ClassificationResult(label = "Class $i", confidence = confidence))
        }
        return results
    }

    companion object {
        const val NUM_CLASSES = 10 // Change to the number of classes in your model
    }
}

data class ClassificationResult(val label: String, val confidence: Float)