/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package com.example.paddleocrlib.yolo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.Log
import com.example.paddleocrlib.utils.Utils
import com.google.android.gms.tflite.gpu.GpuDelegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.util.PriorityQueue
import java.util.Vector

//import org.tensorflow.lite.examples.detection.MainActivity;
//import org.tensorflow.lite.examples.detection.env.Logger;
//import org.tensorflow.lite.examples.detection.env.Utils;
//import org.tensorflow.lite.gpu.GpuDelegate;
/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 *
 *
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
class YoloV5Classifier private constructor() : Classifier {
    override fun enableStatLogging(logStats: Boolean) {}
    override fun getStatString(): String {
        return ""
    }

    override fun close() {
        tfLite?.close()
        tfLite = null
        if (gpuDelegate != null) {
            gpuDelegate?.close()
            gpuDelegate = null
        }
        if (nnapiDelegate != null) {
            nnapiDelegate?.close()
            nnapiDelegate = null
        }
        tfliteModel = null
    }

    override fun setNumThreads(num_threads: Int) {
//        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    override fun setUseNNAPI(isChecked: Boolean) {
//        if (tfLite != null) tfLite.setUseNNAPI(isChecked);
    }

    private fun recreateInterpreter() {
        if (tfLite != null) {
            tfLite?.close()
            tfLite = Interpreter(tfliteModel!!, tfliteOptions)
        }
    }

    fun useGpu() {
        if (gpuDelegate == null) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)
            recreateInterpreter()
        }
    }

    fun useCPU() {
        recreateInterpreter()
    }

    fun useNNAPI() {
        nnapiDelegate = NnApiDelegate()
        tfliteOptions.addDelegate(nnapiDelegate)
        recreateInterpreter()
    }

    var MINIMUM_CONFIDENCE_TF_OD_API = 0.3f
    override fun getObjThresh(): Float {
        return MINIMUM_CONFIDENCE_TF_OD_API
    }

    //    private static final Logger LOGGER = new Logger();
    // Float model
    private val IMAGE_MEAN = 0f
    private val IMAGE_STD = 255.0f

    //config yolo
    var inputSize = -1
        private set

    //    private int[] OUTPUT_WIDTH;
    //    private int[][] MASKS;
    //    private int[] ANCHORS;
    private var output_box = 0
    private var isModelQuantized = false

    /** holds a gpu delegate  */
    var gpuDelegate: GpuDelegate? = null

    /** holds an nnapi delegate  */
    var nnapiDelegate: NnApiDelegate? = null

    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer? = null

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions = Interpreter.Options()

    // Config values.
    // Pre-allocated buffers.
    private val labels = Vector<String>()
    private var intValues: IntArray = intArrayOf()
    private var imgData: ByteBuffer? = null
    private var outData: ByteBuffer? = null
    private var tfLite: Interpreter? = null
    private var inp_scale = 0f
    private var inp_zero_point = 0
    private var oup_scale = 0f
    private var oup_zero_point = 0
    private var numClass = 0

    //non maximum suppression
    protected fun nms(list: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition> {
        val nmsList = ArrayList<Classifier.Recognition>()
        for (k in labels.indices) {
            //1.find max confidence per class
            val pq = PriorityQueue<Classifier.Recognition?>(
                50
            ) { lhs, rhs -> // Intentionally reversed to put high confidence at the head of the queue.
                java.lang.Float.compare(rhs.confidence, lhs.confidence)
            }
            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    pq.add(list[i])
                }
            }

            //2.do non maximum suppression
            while (pq.size > 0) {
                //insert detection with max confidence
                val a = arrayOfNulls<Classifier.Recognition>(pq.size)
                val detections = pq.toArray(a)
                val max = detections[0]!!
                nmsList.add(max)
                pq.clear()
                for (j in 1 until detections.size) {
                    val detection = detections[j]
                    val b = detection!!.location
                    if (box_iou(max.location, b) < mNmsThresh) {
                        pq.add(detection)
                    }
                }
            }
        }
        return nmsList
    }

    protected var mNmsThresh = 0.6f
    protected fun box_iou(a: RectF, b: RectF): Float {
        return box_intersection(a, b) / box_union(a, b)
    }

    protected fun box_intersection(a: RectF, b: RectF): Float {
        val w = overlap((a.left + a.right) / 2, a.right - a.left,
            (b.left + b.right) / 2, b.right - b.left)
        val h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
            (b.top + b.bottom) / 2, b.bottom - b.top)
        return if (w < 0 || h < 0) 0F else w * h
    }

    protected fun box_union(a: RectF, b: RectF): Float {
        val i = box_intersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    protected fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = if (l1 > l2) l1 else l2
        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = if (r1 < r2) r1 else r2
        return right - left
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    protected fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer? {
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
//        byteBuffer.order(ByteOrder.nativeOrder());
//        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val pixel = 0
        imgData!!.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[i * inputSize + j]
                if (isModelQuantized) {
                    // Quantized model
                    imgData!!.put((((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point).toInt().toByte())
                    imgData!!.put((((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point).toInt().toByte())
                    imgData!!.put((((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point).toInt().toByte())
                } else { // Float model
                    imgData!!.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
        return imgData
    }

    override fun recognizeImage(bitmap: Bitmap): ArrayList<Classifier.Recognition> {
        val byteBuffer_ = convertBitmapToByteBuffer(bitmap)
        val outputMap: MutableMap<Int, Any?> = HashMap()

//        float[][][] outbuf = new float[1][output_box][labels.size() + 5];
        outData!!.rewind()
        outputMap[0] = outData
        Log.d("YoloV5Classifier", "mObjThresh: $objThresh")
        val inputArray = arrayOf<Any?>(imgData)
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        val byteBuffer = outputMap[0] as ByteBuffer?
        byteBuffer!!.rewind()
        val detections = ArrayList<Classifier.Recognition>()
        val out = Array(1) { Array(output_box) { FloatArray(numClass + 5) } }
        Log.d("YoloV5Classifier", "out[0] detect start")
        for (i in 0 until output_box) {
            for (j in 0 until numClass + 5) {
                if (isModelQuantized) {
                    out[0][i][j] = oup_scale * ((byteBuffer.get().toInt() and 0xFF) - oup_zero_point)
                } else {
                    out[0][i][j] = byteBuffer.float
                }
            }
            // Denormalize xywh
            for (j in 0..3) {
                out[0][i][j] *= inputSize.toFloat()
            }
        }
        for (i in 0 until output_box) {
            val offset = 0
            val confidence = out[0][i][4]
            var detectedClass = -1
            var maxClass = 0f
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = out[0][i][5 + c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }
            val confidenceInClass = maxClass * confidence
            if (confidenceInClass > objThresh) {
                val xPos = out[0][i][0]
                val yPos = out[0][i][1]
                val w = out[0][i][2]
                val h = out[0][i][3]
                Log.d("YoloV5Classifier",
                    java.lang.Float.toString(xPos) + ',' + yPos + ',' + w + ',' + h)
                val rect = RectF(
                    Math.max(0f, xPos - w / 2),
                    Math.max(0f, yPos - h / 2),
                    Math.min((bitmap.width - 1).toFloat(), xPos + w / 2),
                    Math.min((bitmap.height - 1).toFloat(), yPos + h / 2))
                detections.add(Classifier.Recognition("" + offset, labels[detectedClass],
                    confidenceInClass, rect, detectedClass))
            }
        }
        Log.d("YoloV5Classifier", "detect end")
        //        final ArrayList<Recognition> recognitions = detections;
        return nms(detections)
    }

    fun checkInvalidateBox(x: Float, y: Float, width: Float, height: Float, oriW: Float, oriH: Float, intputSize: Int): Boolean {
        // (1) (x, y, w, h) --> (xmin, ymin, xmax, ymax)
        val halfHeight = height / 2.0f
        val halfWidth = width / 2.0f
        val pred_coor = floatArrayOf(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)

        // (2) (xmin, ymin, xmax, ymax) -> (xmin_org, ymin_org, xmax_org, ymax_org)
        val resize_ratioW = 1.0f * intputSize / oriW
        val resize_ratioH = 1.0f * intputSize / oriH
        val resize_ratio = if (resize_ratioW > resize_ratioH) resize_ratioH else resize_ratioW //min
        val dw = (intputSize - resize_ratio * oriW) / 2
        val dh = (intputSize - resize_ratio * oriH) / 2
        pred_coor[0] = 1.0f * (pred_coor[0] - dw) / resize_ratio
        pred_coor[2] = 1.0f * (pred_coor[2] - dw) / resize_ratio
        pred_coor[1] = 1.0f * (pred_coor[1] - dh) / resize_ratio
        pred_coor[3] = 1.0f * (pred_coor[3] - dh) / resize_ratio

        // (3) clip some boxes those are out of range
        pred_coor[0] = if (pred_coor[0] > 0) pred_coor[0] else 0f
        pred_coor[1] = if (pred_coor[1] > 0) pred_coor[1] else 0f
        pred_coor[2] = if (pred_coor[2] < oriW - 1) pred_coor[2] else oriW - 1
        pred_coor[3] = if (pred_coor[3] < oriH - 1) pred_coor[3] else oriH - 1
        if (pred_coor[0] > pred_coor[2] || pred_coor[1] > pred_coor[3]) {
            pred_coor[0] = 0f
            pred_coor[1] = 0f
            pred_coor[2] = 0f
            pred_coor[3] = 0f
        }

        // (4) discard some invalid boxes
        val temp1 = pred_coor[2] - pred_coor[0]
        val temp2 = pred_coor[3] - pred_coor[1]
        val temp = temp1 * temp2
        if (temp < 0) {
            Log.e("checkInvalidateBox", "temp < 0")
            return false
        }
        if (Math.sqrt(temp.toDouble()) > Float.MAX_VALUE) {
            Log.e("checkInvalidateBox", "temp max")
            return false
        }
        return true
    }

    companion object {
        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param assetManager  The asset manager to be used to load assets.
         * @param modelFilename The filepath of the model GraphDef protocol buffer.
         * @param labelFilename The filepath of label file for classes.
         * @param isQuantized   Boolean representing model is quantized or not
         */
        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String?,
            labelFilename: String,
            isQuantized: Boolean,
            inputSize: Int /*final int[] output_width,
            final int[][] masks,
            final int[] anchors*/
        ): YoloV5Classifier {
            val d = YoloV5Classifier()
            val actualFilename = labelFilename.split("file:///android_asset/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            Log.i("hello" ,"actualFilename  ${actualFilename}")
//            val labelsInput = assetManager.open(actualFilename)
//            val br = BufferedReader(InputStreamReader(labelsInput))
//            var line: String
//            while (br.readLine().also { line = it } != null) {
//                d.labels.add(line)
//            }
//            br.close()

            d.labels.add("signature")
            try {
                val options = Interpreter.Options()
                options.numThreads = NUM_THREADS
                if (isNNAPI) {
                    d.nnapiDelegate = null
                    // Initialize interpreter with NNAPI delegate for Android Pie or above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        d.nnapiDelegate = NnApiDelegate()
                        options.addDelegate(d.nnapiDelegate)
                        options.numThreads = NUM_THREADS
                        //                    options.setUseNNAPI(false);
//                    options.setAllowFp16PrecisionForFp32(true);
//                    options.setAllowBufferHandleOutput(true);
                        options.useNNAPI = true
                    }
                }
                if (isGPU) {
//                GpuDelegate.Options gpu_options = new GpuDelegate.Options();
//                gpu_options.setPrecisionLossAllowed(true); // It seems that the default is true
//                gpu_options.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED);
//                d.gpuDelegate = new GpuDelegate(gpu_options);
//                options.addDelegate(d.gpuDelegate);
                }
                Log.i("hello" ,"called 1")
                d.tfliteModel = Utils.loadModelFile(assetManager, modelFilename)
                Log.i("hello" ,"d.tfliteModel  ${d.tfliteModel}")
                d.tfliteModel?.let {
                    d.tfLite = Interpreter(it, options )
                }

            } catch (e: Exception) {
                Log.i("hello" ,"exception " ,e)
//                throw RuntimeException(e)
            } catch (e: Error) {
                Log.i("hello" ,"exception " ,e)
//                throw RuntimeException(e)
            }
            d.isModelQuantized = isQuantized
            // Pre-allocate buffers.
            val numBytesPerChannel: Int
            numBytesPerChannel = if (isQuantized) {
                1 // Quantized
            } else {
                4 // Floating point
            }
            d.inputSize = inputSize
            d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel)
            d.imgData?.order(ByteOrder.nativeOrder())
            d.intValues = IntArray(d.inputSize * d.inputSize)
            d.output_box = ((Math.pow((inputSize / 32).toDouble(), 2.0) + Math.pow((inputSize / 16).toDouble(), 2.0) + Math.pow((inputSize / 8).toDouble(), 2.0)) * 3).toInt()
            //        d.OUTPUT_WIDTH = output_width;
//        d.MASKS = masks;
//        d.ANCHORS = anchors;
            if (d.tfLite != null) {
                if (d.isModelQuantized) {
                    val inpten = d.tfLite!!.getInputTensor(0)
                    d.inp_scale = inpten.quantizationParams().scale
                    d.inp_zero_point = inpten.quantizationParams().zeroPoint
                    val oupten = d.tfLite!!.getOutputTensor(0)
                    d.oup_scale = oupten.quantizationParams().scale
                    d.oup_zero_point = oupten.quantizationParams().zeroPoint
                }
                val shape = d.tfLite!!.getOutputTensor(0).shape()
                val numClass = shape[shape.size - 1] - 5
                d.numClass = numClass
                d.outData = ByteBuffer.allocateDirect(d.output_box * (numClass + 5) * numBytesPerChannel)
                d.outData?.order(ByteOrder.nativeOrder())
            }
            return d
        }

        private val XYSCALE = floatArrayOf(1.2f, 1.1f, 1.05f)
        private const val NUM_BOXES_PER_BLOCK = 3

        // Number of threads in the java app
        private const val NUM_THREADS = 1
        private const val isNNAPI = false
        private const val isGPU = false
        protected const val BATCH_SIZE = 1
        protected const val PIXEL_SIZE = 3
    }
}