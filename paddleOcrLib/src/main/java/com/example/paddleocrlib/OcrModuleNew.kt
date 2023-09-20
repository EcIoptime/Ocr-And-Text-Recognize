package com.example.paddleocrlib

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.darwin.viola.still.FaceDetectionListener
import com.darwin.viola.still.Viola
import com.darwin.viola.still.model.CropAlgorithm
import com.darwin.viola.still.model.FaceDetectionError
import com.darwin.viola.still.model.FaceOptions
import com.darwin.viola.still.model.Result
import com.example.paddleocrlib.yolo.YoloV5Classifier
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt


public class OcrModuleNew {

    companion object {

        private var faceMatchClassifier: ImageClassificationHelper? = null
        var detectorFile: YoloV5Classifier? = null

        @JvmStatic
        public fun OcrModuleInit(context: Context, callBack: ((BaseResult, Bitmap) -> Unit)) {
            val intent = OcrModule.doOcr(context) { result: BaseResult, cropedImage: Bitmap ->
                callBack.invoke(result, cropedImage)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun checkIfNerRequirementsGood(lifecycleCoroutineScope: LifecycleCoroutineScope, context: Context, isSuccess: () -> Unit) {
            lifecycleCoroutineScope.launch(Dispatchers.IO) {
                try {
                    if (!Python.isStarted())
                        Python.start(AndroidPlatform(context))
                } catch (_: Exception) {
                }

                if (!File(MyPrefManager.getInstance(context!!)?.modelKenyaIdCardFrontPath).exists()) {
                    val filePath = ImageUtils.copyZipFromAssetsToCacheAndUnzip(context, "kenya_id_card_front.zip")
                    MyPrefManager.getInstance(context!!)?.modelKenyaIdCardFrontPath = filePath ?: ""
                    Log.i("hello", "copyied file ${filePath}")
                }

                if (!File(MyPrefManager.getInstance(context!!)?.modelKenyaIdCardBackPath).exists()) {
                    val filePath = ImageUtils.copyZipFromAssetsToCacheAndUnzip(context, "kenya_id_card_back.zip")
                    MyPrefManager.getInstance(context!!)?.modelKenyaIdCardBackPath = filePath ?: ""
                    Log.i("hello", "copyied file ${filePath}")
                }

                if (!File(MyPrefManager.getInstance(context!!)?.modelAllCardPath).exists()) {
                    val filePath = ImageUtils.copyZipFromAssetsToCacheAndUnzip(context, "model_text_classifcation.zip")
                    MyPrefManager.getInstance(context!!)?.modelAllCardPath = filePath ?: ""
                    Log.i("hello", "copyied file ${filePath}")
                }

                lifecycleCoroutineScope.launch(Dispatchers.Main) {
                    Log.i("test", "222 ${isSuccess}")

                    isSuccess.invoke()
                }

            }
        }

        var isTfliteInitialized: Boolean = false

        @JvmStatic
        fun extractFaceFromImage(photo: Bitmap, faceBitmap: (Bitmap?) -> Unit) {

            var bitmapFace = photo
            bitmapFace = bitmapFace.getHeightOfWidth(1080.0)

            val listener: FaceDetectionListener = object : FaceDetectionListener {
                override fun onFaceDetected(result: Result) {
                    result.facePortraits.firstOrNull()?.let { face ->
                        faceBitmap.invoke(face.face)
                    } ?: kotlin.run { faceBitmap.invoke(null) }
                }

                override fun onFaceDetectionFailed(error: FaceDetectionError, message: String) {
                    faceBitmap.invoke(null)
                    Log.i("test", " error ${error.message}")
                }
            }

            val viola = Viola(listener)
            val faceOption = FaceOptions.Builder()
//            .enableProminentFaceDetection()
                .enableDebug()
                .setMinimumFaceSize(10)
                .cropAlgorithm(CropAlgorithm.THREE_BY_FOUR)
                .build()

            viola.detectFace(bitmapFace, faceOption)
        }

        @JvmStatic
        fun findCosineDistance(sourceRepresentation: FloatArray, testRepresentation: FloatArray): Float {
            val a = sourceRepresentation.indices.sumByDouble { sourceRepresentation[it].toDouble() * testRepresentation[it].toDouble() }
            val b = sourceRepresentation.sumByDouble { it.toDouble() * it.toDouble() }
            val c = testRepresentation.sumByDouble { it.toDouble() * it.toDouble() }

            return abs((/*1 -*/ (a / (sqrt(b) * sqrt(c))).toFloat()) * 100f)
        }

        @JvmStatic
        fun matchFaces(context: Context , face1: Bitmap, face2: Bitmap, scoreMatchCallBack: (Float) -> Unit) {
            if (faceMatchClassifier == null) {
                faceRecognizeInit(context) {
                    matchFacesHelper(face1, face2, scoreMatchCallBack)
                }
            }else{
                matchFacesHelper(face1, face2, scoreMatchCallBack)
            }
        }

        private fun matchFacesHelper(face1: Bitmap, face2: Bitmap, scoreMatchCallBack: (Float) -> Unit) {
            val embedding1 = faceMatchClassifier?.classify(face1, 0)
            val embedding2 = faceMatchClassifier?.classify(face2, 0)

            if (embedding1 != null && embedding2 != null) {
                var scoreMatch = findCosineDistance(embedding1, embedding2)
                scoreMatchCallBack.invoke(scoreMatch)
            }
        }

        @JvmStatic
        fun faceRecognizeInit(context: Context , initSucessfull:(Boolean)->Unit){

            var MAX_REPORT = 3
            var useGpu = false;
            // Initialize TFLite once. Must be called before creating the classifier
            val initializeTask: Task<Void> by lazy {
                TfLite.initialize(
                    context,
                    TfLiteInitializationOptions.builder()
                        .setEnableGpuDelegateSupport(false)
                        .build()
                ).continueWithTask { task ->
                    if (task.isSuccessful) {
                        useGpu = false;
                        return@continueWithTask Tasks.forResult(null)
                    } else {
                        // Fallback to initialize interpreter without GPU
                        return@continueWithTask TfLite.initialize(context)
                    }
                }.addOnFailureListener {
                    initSucessfull.invoke(false)
                }
            }

            if (isTfliteInitialized) {
                faceMatchClassifier = ImageClassificationHelper(context, MAX_REPORT, useGpu)
                initSucessfull.invoke(true)
            } else {
                initializeTask.addOnSuccessListener {
                    isTfliteInitialized = true
                    faceMatchClassifier = ImageClassificationHelper(context, MAX_REPORT, useGpu)
                    initSucessfull.invoke(true)
                }
            }
        }

    }



}