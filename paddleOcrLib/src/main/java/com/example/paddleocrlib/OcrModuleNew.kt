package com.example.paddleocrlib

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors


public class OcrModuleNew {

    companion object{

        @JvmStatic
        public fun OcrModuleInit(context: Context ,  callBack:((BaseResult , Bitmap)->Unit)){
            val intent  = OcrModule.doOcr(context) { result:BaseResult, cropedImage:Bitmap ->
                callBack.invoke(result , cropedImage)
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

    }

    private var classifier: ImageClassificationHelper? = null
    fun faceRecognizeInit(context: Context): OcrModuleNew {

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
            }
                .addOnFailureListener {
                }
        }


        initializeTask
            .addOnSuccessListener {
                classifier = ImageClassificationHelper(context, MAX_REPORT, useGpu)
            }

        return this
    }
}