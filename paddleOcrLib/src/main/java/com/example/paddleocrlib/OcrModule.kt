package com.example.paddleocrlib

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.darwin.viola.still.FaceDetectionListener
import com.darwin.viola.still.Viola
import com.darwin.viola.still.model.FaceDetectionError
import com.darwin.viola.still.model.FaceOptions
import com.darwin.viola.still.model.Result
import com.example.paddleocrlib.databinding.ActivityMainOcrBinding
import com.example.paddleocrlib.yolo.DetectorFactory
import com.example.paddleocrlib.yolo.YoloV5Classifier
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates


class OcrModule : AppCompatActivity() {

    private var objectDetectorHelper: ObjectDetectorHelper? = null
    private var classifier: SignatureClassification? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var useGpu = false
    var detectorFile: YoloV5Classifier? = null


    // Initialize TFLite once. Must be called before creating the classifier
    private val initializeTask: Task<Void> by lazy {
        TfLite.initialize(
            this,
            TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(true)
                .build()
        ).continueWithTask { task ->
            if (task.isSuccessful) {
                useGpu = false//true
                return@continueWithTask Tasks.forResult(null)
            } else {
                // Fallback to initialize interpreter without GPU
                return@continueWithTask TfLite.initialize(this)
            }
        }
            .addOnFailureListener {
                Log.e("test", "TFLite in Play Services failed to initialize.", it)
            }
    }

    private fun signatureDetectLogic() {
        // Initialize TFLite asynchronously
        initializeTask
            .addOnSuccessListener {
                Log.i("test", "TFLite in Play Services initialized successfully.")
                classifier = SignatureClassification(this, MAX_REPORT, useGpu)
//                val embedding1 = classifier?.classify(face1, 0)
            }

    }

    var ocrResult: BaseResult? = null
    var ocrObj: OcrPredictionForImageTextUpdated? = null
    var binding: ActivityMainOcrBinding? = null
    private lateinit var cameraExecutor: ExecutorService
    private val REQUEST_CAMERA_PERMISSION = 100

    //    var currentScanType = CardScanningType.Front
    enum class CardScanningType { Front, Back }

    // Use Delegates.observable to observe changes to 'myProperty'
    var currentScanType by Delegates.observable(CardScanningType.Front) { _, oldValue, newValue ->
        lifecycleScope.launch(Dispatchers.Main) {
            if (newValue == CardScanningType.Front) {
                binding?.cardScanType?.text = "Please Place the front side\nof Id Card in Card Preview"
            } else if (newValue == CardScanningType.Back) {
                binding?.cardScanType?.text = "Please Place the back side\nof Id Card in Card Preview"
            }
        }
    }


    var viewFinder: PreviewView? = null
    var imageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainOcrBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        supportActionBar?.hide()


        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder = binding?.viewFinder
        imageView = binding?.imageView

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            // Permission is already granted, you can start using the camera
            setupCamera()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            ocrObj = OcrPredictionForImageTextUpdated(this@OcrModule, 0.5f) {
            }



//            if(!signatureModuleInitialized.get()) {
//                initializeTask
//                    .addOnSuccessListener {
//                        Log.i("test", "TFLite in Play Services initialized successfully.")
//                        classifier = SignatureClassification(this@OcrModule, MAX_REPORT, useGpu)
//                        signatureModuleInitialized.set(true)
//                    }
//            }
        }
//        objectDetectorHelper = ObjectDetectorHelper(
//            context = this,
//            objectDetectorListener = object : ObjectDetectorHelper.DetectorListener {
//                override fun onInitialized() {
//                    Log.i("test", "init sucess")
//                }
//
//                override fun onError(error: String) {
//                    Log.i("test", "init ${error}")
//                }
//
//                override fun onResults(results: MutableList<Detection>?, inferenceTime: Long, imageHeight: Int, imageWidth: Int) {
//
//                }
//            })

        detectorFile = DetectorFactory.getDetector(assets, "signature.tflite")

    }

    var signatureModuleInitialized = AtomicBoolean(false)

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, you can start using the camera
                setupCamera()
            } else {
                // Permission was denied, inform the user or disable camera-related features
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCamera() {
        Log.i("test", "camera setup ")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .setTargetRotation(binding?.viewFinder?.display?.rotation ?: 0)
            .build()
            .also {
                it.setSurfaceProvider(viewFinder?.surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding?.viewFinder?.display?.rotation ?: 0)
            .build()
            .also { imageAnalysis ->
                viewFinder?.also { viewFinder ->
                    viewFinder.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            val viewTreeObserver = binding?.cardLayoutSample?.viewTreeObserver
                            viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    // Get the layout width
                                    val layoutWidth = binding?.cardLayoutSample?.width!!
                                    val layoutHeight = binding?.cardLayoutSample?.height!!

                                    imageAnalysis.setAnalyzer(cameraExecutor, CustomImageAnalyzer(viewFinder, this@OcrModule, { bitmapOriginal ->
//                                        return@CustomImageAnalyzer

//                                        Log.i("hello" ,"signatureModuleInitialized ${signatureModuleInitialized.get()}")

//                                        if (!signatureModuleInitialized.get()) {
//                                            isProcessing.set(false)
//                                            return@CustomImageAnalyzer
//                                        }


                                        lifecycleScope.launch(Dispatchers.IO) {

                                            val bitmap = bitmapOriginal.copy(bitmapOriginal.config, true)

                                            if (ocrObj?.isInitialized() == false) {
                                                isProcessing.set(false)
                                                return@launch
                                            }
                                            var result = ocrObj?.processImage(bitmap)
                                            lifecycleScope.launch(Dispatchers.Main) {
//                                                binding?.probabilityType?.text = "${result?.showDataCard}"
                                            }

                                            Log.i("hello", "currentScanType ${currentScanType}  ${result?.name}")

                                            if (currentScanType == CardScanningType.Front) {

                                                if (result?.name.isNullOrEmpty()) {
                                                    isProcessing.set(false)
                                                    return@launch
                                                }

                                                extractFaceFromImage(bitmap) { face ->
                                                    if (face == null) {
                                                        isProcessing.set(false)
                                                        return@extractFaceFromImage
                                                    }


                                                    ocrResult = result?.copy()
                                                    ocrResult?.cardFrontSideImage = bitmapOriginal
                                                    ocrResult?.faceImage = face
                                                    ocrResult?.frontCardData = result?.rawLine
                                                    ocrResult?.frontCardProbability = result?.cardProbability

                                                    extractSignature(bitmapOriginal)?.let { ocrResult?.signature?.add(it) }

                                                    currentScanType = CardScanningType.Back
                                                    Handler(mainLooper).post {
                                                        Toast.makeText(this@OcrModule, "Please switch the card to back side", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isProcessing.set(false)
                                                }


//                                                ocrResult = result?.copy()
//                                                currentScanType = CardScanningType.Back
//                                                isProcessing.set(false)
//                                                return@launch

                                            } else if (currentScanType == CardScanningType.Back) {
                                                if (result?.district.isNullOrEmpty()) {
                                                    isProcessing.set(false)
                                                    return@launch
                                                }

                                                ocrResult?.backCardProbability = result?.cardProbability
                                                ocrResult?.backCardData = result?.rawLine
                                                ocrResult?.district = result?.district ?: ""
                                                ocrResult?.division = result?.division ?: ""
                                                ocrResult?.location = result?.location ?: ""
                                                ocrResult?.subLocation = result?.subLocation ?: ""
                                                ocrResult?.identifier = result?.identifier ?: ""
                                                ocrResult?.cardBackSideImage = bitmapOriginal

                                                extractSignature(bitmapOriginal)?.let { ocrResult?.signature?.add(it) }

                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    ocrResult?.let { callBack?.invoke(it, bitmapOriginal) }
                                                    cameraExecutor.shutdown()
                                                    isProcessing.set(false)
                                                    callBack = null
                                                    finish()
                                                }
                                            }
                                            return@launch

                                            if (result?.district.isNullOrEmpty()) {
                                                isProcessing.set(false)
                                                return@launch
                                            }


//                                                result?.cardProfile = face
//                                                result?.cardImage = bitmap


                                            extractFaceFromImage(bitmap) { face ->
                                                if (face == null) {
                                                    isProcessing.set(false)
                                                    return@extractFaceFromImage
                                                }
                                                result?.cardProfile = face
                                                result?.cardImage = bitmap
                                                result?.let { callBack?.invoke(it, face) }
                                                cameraExecutor.shutdown()
                                                isProcessing.set(false)
                                                callBack = null
                                                finish()
                                            }


//                                            OcrPredictionForImageTextNew(bitmap , this@OcrModule, 0.5f) { result ->
//
//                                                lifecycleScope.launch(Dispatchers.Main) {
//                                                    binding?.probabilityType?.text = "${result?.showDataCard}"
//                                                }
//
//                                                Log.i("test" ,"hurrah ${result?.cardType}  ${result?.name}")
//                                                Log.i("test" ,"hurrah ${result?.licenseNumber}  ${result?.dateOfBirth}")
//
//                                                if (result?.name.isNullOrEmpty()) {
//                                                    isProcessing.set(false)
//                                                    return@OcrPredictionForImageTextNew
//                                                }
//
////                                                if (result?.district.isNullOrEmpty()) {
////                                                    isProcessing.set(false)
////                                                    return@OcrPredictionForImageTextNew
////                                                }
//
//
////                                                result?.cardProfile = face
////                                                result?.cardImage = bitmap
//                                                result?.let { callBack?.invoke(it, bitmapOriginal) }
//                                                cameraExecutor.shutdown()
//                                                isProcessing.set(false)
//                                                callBack = null
//                                                finish()
//                                                return@OcrPredictionForImageTextNew
//
//                                                extractFaceFromImage(bitmap){ face ->
//                                                    if (face ==null ) {
//                                                        isProcessing.set(false)
//                                                        return@extractFaceFromImage
//                                                    }
//                                                    result?.cardProfile = face
//                                                    result?.cardImage = bitmap
//                                                    result?.let { callBack?.invoke(it, face) }
//                                                    cameraExecutor.shutdown()
//                                                    isProcessing.set(false)
//                                                    callBack = null
//                                                    finish()
//                                                }
//
//                                            }
                                        }

                                    }, lifecycleScope, layoutWidth, layoutHeight, {
                                        binding?.errorMessages?.text = it
//                                        if (binding?.errorMessages?.text?.isNotEmpty() ==true) {
//                                        }else{
//                                            binding?.errorMessages?.text = it
//                                        }

                                    }, { bitmap ->

                                        runOnUiThread {

//                                            imageView?.setImageBitmap(bitmap)
                                        }
                                    }, { result, bitmap ->
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            binding?.probabilityType?.text = "${result.cardProbability}"
                                            result.cardImage?.let {
                                                cameraExecutor.shutdown()
                                                callBack?.invoke(result, bitmap)
                                                isProcessing.set(false)
                                                callBack = null
                                                finish()
                                            }
                                        }
                                    }))

                                    // Remove the global layout listener to avoid multiple calls
                                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                                }
                            })

                            // Remove the global layout listener to avoid multiple calls
                            viewFinder.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    })
                }
            }
        try {
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.i("test", "Use case binding failed", e)
        }
    }

    private fun extractSignature(bitmapOriginal: Bitmap): Bitmap? {
        var bitmapData:Bitmap?=null
        try {
            val cropSize: Int = detectorFile?.inputSize ?: 0
            val frameToCropTransform = ImageUtilsYolo.getTransformationMatrix(bitmapOriginal.width, bitmapOriginal.height, cropSize, cropSize, 0, false)

            val cropToFrameTransform = Matrix()
            frameToCropTransform.invert(cropToFrameTransform)
            val croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(croppedBitmap)
            canvas.drawBitmap(bitmapOriginal, frameToCropTransform, null)
            val signatureResults = detectorFile?.recognizeImage(croppedBitmap)
            signatureResults?.firstOrNull()?.let { signature ->

                val leftX = (bitmapOriginal.width / cropSize.toFloat()) * signature.location.left
                val rightX = (bitmapOriginal.width / cropSize.toFloat()) * signature.location.width()

                val topX = (bitmapOriginal.height / cropSize.toFloat()) * signature.location.top
                val bottomX = (bitmapOriginal.height / cropSize.toFloat()) * signature.location.height()

                val portionBitmap = Bitmap.createBitmap(bitmapOriginal, leftX.toInt(), topX.toInt(), rightX.toInt(), bottomX.toInt())
                bitmapData = portionBitmap

            }
        } catch (e: Exception) {
            Log.i("hello", "signatureResults size  ", e)
        }
        return bitmapData
    }

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
//                Toast.makeText(this@OcrModule, "No face detected, please try again", Toast.LENGTH_SHORT).show()
                faceBitmap.invoke(null)
                Log.i("test", " error ${error.message}")
            }
        }

        val viola = Viola(listener)
//            viola.addAgeClassificationPlugin(this) //optional, available via external dependency
        val faceOption = FaceOptions.Builder()
//            .enableProminentFaceDetection()
            .enableDebug()
            .setMinimumFaceSize(10)
//            .cropAlgorithm(CropAlgorithm.THREE_BY_FOUR)
            .build()

        viola.detectFace(bitmapFace, faceOption)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        var callBack: ((BaseResult, Bitmap) -> Unit)? = null

        @JvmStatic
        fun doOcr(context: Context, callBack: ((BaseResult, Bitmap) -> Unit)): Intent {
            this.callBack = callBack
            return Intent(context, OcrModule::class.java)
        }

        private const val MAX_REPORT = 3
    }
}


fun Bitmap.getHeightOfWidth(maxProportion: Double): Bitmap {

    var newBitmap: Bitmap = this
//        if (newBitmap.width < 500){
    val scaledHeight = Math.ceil(maxProportion * (this.height.toFloat() / this.width.toFloat())).toInt()
//                val scaledWidth = Math.ceil(scaledHeight.toDouble() * it.getWidth() / it.getHeight()).toInt()
    newBitmap = Bitmap.createScaledBitmap(this, maxProportion.toInt(), scaledHeight.toInt(), true)
//        }

    if (newBitmap.height < 500) {
        val scaledWidth = Math.ceil(maxProportion.toDouble() * (this.width / this.height.toFloat())).toInt()
        newBitmap = Bitmap.createScaledBitmap(this, scaledWidth, maxProportion.toInt(), true)
    }
//        val scaledHeight = Math.ceil(maxWidth * it.height / it.width).toInt()
////                val scaledWidth = Math.ceil(scaledHeight.toDouble() * it.getWidth() / it.getHeight()).toInt()
//        val newBitmap = Bitmap.createScaledBitmap(it, maxWidth.toInt(), scaledHeight.toInt(), true)
    return newBitmap
}