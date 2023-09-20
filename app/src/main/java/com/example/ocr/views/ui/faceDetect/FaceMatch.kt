package com.example.ocr.views.ui.faceDetect

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ocr.databinding.ActivityFaceMatchBinding
import com.example.paddleocrlib.OcrModuleNew
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

class FaceMatch : AppCompatActivity() {

    var face1:Bitmap?= null
    var face2:Bitmap?= null

    companion object {
        private val TAG = "test"
        const val MAX_REPORT = 3
    }

    var binding: ActivityFaceMatchBinding? = null
//    private val executor = Executors.newSingleThreadExecutor()
//    private var useGpu = false;

    // Initialize TFLite once. Must be called before creating the classifier
//    private val initializeTask: Task<Void> by lazy {
//        TfLite.initialize(
//            this,
//            TfLiteInitializationOptions.builder()
//                .setEnableGpuDelegateSupport(true)
//                .build()
//        ).continueWithTask { task ->
//            if (task.isSuccessful) {
//                useGpu = true;
//                return@continueWithTask Tasks.forResult(null)
//            } else {
//                // Fallback to initialize interpreter without GPU
//                return@continueWithTask TfLite.initialize(this)
//            }
//        }
//            .addOnFailureListener {
//                Log.e(TAG, "TFLite in Play Services failed to initialize.", it)
//            }
//    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceMatchBinding.inflate(layoutInflater)
        setContentView(binding?.root)
//        setContentView(R.layout.activity_face_match)
        inits()
    }

    private fun inits() {

        binding?.uploadFace1?.setOnClickListener { uploadFace1Logic() }
        binding?.uploadFace2?.setOnClickListener { uploadFace2Logic() }
        binding?.faceMatch?.setOnClickListener { faceMatchLogic() }

        // Initialize TFLite asynchronously
//        initializeTask
//            .addOnSuccessListener {
//                Log.i("test", "TFLite in Play Services initialized successfully.")
//                classifier = ImageClassificationHelper(this, MAX_REPORT, useGpu)
//            }
    }

    private fun uploadFace1Logic() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncherFace1.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        }else{
                // Check if permission is granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openMediaPickerFace1()
            } else {
                // Request the permission
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)

                requestPermissionLauncherFace1.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

    }


    val getContentLauncherFace1 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Handle the selected image URI
            // You can use this URI to load the image or perform any required operations
            face1 = getBitmapFromUri(it)
            binding?.face1?.setImageBitmap(face1)
        }
    }

    // Request permission to read external storage
    val requestPermissionLauncherFace1 = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, open media picker
            openMediaPickerFace1()
        } else {
            // Permission denied, handle this case
        }
    }
    // Call this function to open the media picker
    private fun openMediaPickerFace1() {
        getContentLauncherFace1.launch("image/*")
    }

    private fun uploadFace2Logic() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncherFace2.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        }else{
            // Check if permission is granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openMediaPickerFace2()
            } else {
                // Request the permission
                requestPermissionLauncherFace2.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

    }


    val getContentLauncherFace2 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Handle the selected image URI
            // You can use this URI to load the image or perform any required operations
            face2 =  getBitmapFromUri(it)
            binding?.face2?.setImageBitmap(face2)
        }
    }

    // Request permission to read external storage
    val requestPermissionLauncherFace2 = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, open media picker
            openMediaPickerFace2()
        } else {
            // Permission denied, handle this case
        }
    }
    // Call this function to open the media picker
    private fun openMediaPickerFace2() {
        getContentLauncherFace2.launch("image/*")
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            // Open an input stream from the URI and decode it to a Bitmap
            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inSampleSize = 1 // You can adjust this to control image quality vs. memory usage
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    var matchingJob:Job = Job()
    private fun faceMatchLogic() {
        if (face1 ==null || face2 ==null){
            Toast.makeText(this,"Please upload both face",Toast.LENGTH_SHORT).show()
            return
        }
        var currentTime = System.currentTimeMillis()
        // Perform the image classification for the current frame
        try {
            matchingJob.cancel()
        } catch (e: Exception) {
        }
        matchingJob = lifecycleScope.launch(Dispatchers.IO) {
            face1/*loadImageFromAssets(this@FaceMatch, "face5.png")*/?.let {imag1->
                OcrModuleNew.extractFaceFromImage(imag1) { face1 ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding?.face1?.setImageBitmap(face1)
                    }
                    face2/*loadImageFromAssets(this@FaceMatch, "face6.png")*/?.let {imag2->
                        OcrModuleNew.extractFaceFromImage(imag2) { face2 ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding?.face2?.setImageBitmap(face2)
                            }
                            if (face1 != null && face2 != null) {
                                OcrModuleNew.matchFaces(this@FaceMatch ,face1 ,face2) { scoreMatch ->
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        val tt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() -currentTime)
                                        if(scoreMatch > 40)
                                            binding?.faceMatchText?.text = "Face Matched"
                                        else
                                            binding?.faceMatchText?.text = "Face Not Matched"
                                        binding?.processedText?.text = "Processed Time: ${tt} seconds"
                                        Log.i("test", " score matched ${scoreMatch}")
                                    }
                                }
//                                val embedding1 = classifier?.classify(face1, 0)
////                                val embeddingTemp1 = embedding1?.map { it }?.joinToString { it.toString() }
////                                showLogs("test",embeddingTemp1 ?: "")
//
//                                val embedding2 = classifier?.classify(face2, 0)
//                                if (embedding1 != null && embedding2 != null) {
//                                    var scoreMatch = findCosineDistance(embedding1, embedding2)
//                                    lifecycleScope.launch(Dispatchers.Main) {
//                                        val tt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() -currentTime)
//                                        if(scoreMatch > 40)
//                                            binding?.faceMatchText?.text = "Face Matched"
//                                        else
//                                            binding?.faceMatchText?.text = "Face Not Matched"
//                                        binding?.processedText?.text = "Processed Time: ${tt} seconds"
//                                        Log.i("test", " score matched ${scoreMatch}")
//                                    }
//                                }

                            }

                        }

                    }


                }
            }

            //            Log.i("test", "\n embedding1 ${embeddingTemp1}")
//            Log.i("test", "\n embedding1 ${embedding1?.size}")

//            val embedding1 = loadImageFromAssets(this@FaceMatch, "face4.png")?.let {
//                classifier?.classify(it, 0)
//            }
//            val embedding2 = loadImageFromAssets(this@FaceMatch, "face1.png")?.let {
//                classifier?.classify(it, 0)
//            }
//            lifecycleScope.launch(Dispatchers.Main) {
//                if (embedding1 != null && embedding2 != null) {
//                    var scoreMatch = findCosineDistance(embedding1, embedding2)
//                    val tt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() -currentTime)
//                    binding?.faceMatchText?.text = "Face Matched : ${scoreMatch}"
//                    binding?.processedText?.text = "Processed Time: ${tt} seconds"
////                    Log.i("test", " score matched ${scoreMatch}")
//                }
//            }

        }


    }

    fun showLogs(tag: String, veryLongString: String) {
        val maxLogSize = 1000
        for (i in 0..veryLongString.length / maxLogSize) {
            val start = i * maxLogSize
            var end = (i + 1) * maxLogSize
            end = if (end > veryLongString.length) veryLongString.length else end
            Log.i(tag, veryLongString.substring(start, end))
        }
    }

    fun loadImageFromAssets(context: Context, assetFileName: String): Bitmap? {
        try {
            // Open an input stream for the asset
            val inputStream = context.assets.open(assetFileName)

            // Decode the input stream into a Bitmap
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun findCosineDistance(sourceRepresentation: FloatArray, testRepresentation: FloatArray): Float {
        val a = sourceRepresentation.indices.sumByDouble { sourceRepresentation[it].toDouble() * testRepresentation[it].toDouble() }
        val b = sourceRepresentation.sumByDouble { it.toDouble() * it.toDouble() }
        val c = testRepresentation.sumByDouble { it.toDouble() * it.toDouble() }

        return abs((/*1 -*/ (a / (sqrt(b) * sqrt(c))).toFloat()) * 100f)
    }

//    fun extractFaceFromImage(photo: Bitmap, faceBitmap: (Bitmap?) -> Unit) {
//
//        var bitmapFace = photo
//        bitmapFace = bitmapFace.getHeightOfWidth(1080.0)
//
//        val listener: FaceDetectionListener = object : FaceDetectionListener {
//            override fun onFaceDetected(result: Result) {
//                result.facePortraits.firstOrNull()?.let { face ->
//                    faceBitmap.invoke(face.face)
//                } ?: kotlin.run { faceBitmap.invoke(null) }
//            }
//
//            override fun onFaceDetectionFailed(error: FaceDetectionError, message: String) {
//                faceBitmap.invoke(null)
//                Log.i("test", " error ${error.message}")
//            }
//        }
//
//        val viola = Viola(listener)
//        val faceOption = FaceOptions.Builder()
////            .enableProminentFaceDetection()
//            .enableDebug()
//            .setMinimumFaceSize(10)
//            .cropAlgorithm(CropAlgorithm.THREE_BY_FOUR)
//            .build()
//
//        viola.detectFace(bitmapFace, faceOption)
//    }


    override fun onDestroy() {
        // Terminate all outstanding analyzing jobs (if there is any).
//        executor.apply {
//            shutdown()
//            awaitTermination(1000, TimeUnit.MILLISECONDS)
//        }
        // Release TFLite resources
//        classifier?.close()
        super.onDestroy()
    }
}