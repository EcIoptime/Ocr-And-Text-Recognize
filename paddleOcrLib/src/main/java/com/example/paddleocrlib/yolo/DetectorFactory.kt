package com.example.paddleocrlib.yolo

import android.content.res.AssetManager
import java.io.IOException

object DetectorFactory {
    @Throws(IOException::class)
    fun getDetector(
        assetManager: AssetManager,
        modelFilename: String): YoloV5Classifier {
        var labelFilename: String = ""
        var isQuantized = false
        var inputSize = 0
        var output_width = intArrayOf(0)
        var masks = arrayOf(intArrayOf(0))
        var anchors = intArrayOf(0)
        if (modelFilename == "signature.tflite") {
            labelFilename = "file:///android_asset/customclasses.txt"
            isQuantized = false
            inputSize = 640
            output_width = intArrayOf(80, 40, 20)
            masks = arrayOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8))
            anchors = intArrayOf(
                10, 13, 16, 30, 33, 23, 30, 61, 62, 45, 59, 119, 116, 90, 156, 198, 373, 326
            )
        } else if (modelFilename == "best-fp16.tflite") {
            labelFilename = "file:///android_asset/customclasses.txt"
            isQuantized = false
            inputSize = 416
            output_width = intArrayOf(40, 20, 10)
            masks = arrayOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8))
            anchors = intArrayOf(
                10, 13, 16, 30, 33, 23, 30, 61, 62, 45, 59, 119, 116, 90, 156, 198, 373, 326
            )
        } else if (modelFilename == "yolov5s-int8.tflite") {
            labelFilename = "file:///android_asset/customclasses.txt"
            isQuantized = true
            inputSize = 416
            output_width = intArrayOf(40, 20, 10)
            masks = arrayOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8))
            anchors = intArrayOf(
                10, 13, 16, 30, 33, 23, 30, 61, 62, 45, 59, 119, 116, 90, 156, 198, 373, 326
            )
        }
        return YoloV5Classifier.create(assetManager, modelFilename, labelFilename, isQuantized,
            inputSize)
    }
}