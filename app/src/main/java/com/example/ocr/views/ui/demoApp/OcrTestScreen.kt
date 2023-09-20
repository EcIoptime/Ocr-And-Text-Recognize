package com.example.ocr.views.ui.demoApp

//import za.co.brightspace.bidms_cloud.model.cardScanningDetail.Attribute
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocr.common.Util.showToast
import com.example.ocr.databinding.ActivityOcrTestScreenBinding
import com.example.ocr.utilities.Utility
import com.example.ocr.views.dialogueFragments.SelectOcrTypeDialogue
import com.example.ocr.views.ui.faceDetect.FaceMatch
import com.example.paddleocrlib.BaseResult
import com.example.paddleocrlib.OcrModuleNew
import com.schaefer.livenesscamerax.domain.model.CameraSettings
import com.schaefer.livenesscamerax.domain.model.StepLiveness
import com.schaefer.livenesscamerax.domain.model.StorageType
import com.schaefer.livenesscamerax.navigation.LivenessEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class OcrTestScreen : AppCompatActivity() {

    companion object {
        private const val MAX_REPORT = 3
    }

    private val livenessEntryPoint = LivenessEntryPoint
    var currentScanType: ScanType? = ScanType.OurOcr
    private val mutableStepList = arrayListOf<StepLiveness>()

    enum class ScanType {
        Regula, OurOcr
    }

    var binding: ActivityOcrTestScreenBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrTestScreenBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        inits()
    }

    private fun inits() {

        showOurOcrLayoutLogic()
        binding?.faceDetect?.setOnClickListener { faceDetectLogic() }
        binding?.scanOcr?.setOnClickListener { scanOcrLogic() }
        binding?.showRegulaOcrLayout?.setOnClickListener { showRegulaOcrLayoutLogic() }
        binding?.showOurOcrLayout?.setOnClickListener { showOurOcrLayoutLogic() }
        binding?.selectTextAndData?.setOnClickListener { selectTextAndDataLogic() }
        binding?.livenessCheck?.setOnClickListener { livenessCheckLogic() }
//        binding?.signatureDetect?.setOnClickListener { signatureDetectLogic() }

//        initializeTask
//            .addOnSuccessListener {
//                Log.i("test", "TFLite in Play Services initialized successfully.")
//                classifier = ImageClassificationHelper(this, FaceMatch.MAX_REPORT, useGpu)
//            }

    }

//    private var classifier: ImageClassificationHelper? = null
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
//                useGpu = false;
//                return@continueWithTask Tasks.forResult(null)
//            } else {
//                // Fallback to initialize interpreter without GPU
//                return@continueWithTask TfLite.initialize(this)
//            }
//        }
//            .addOnFailureListener {
//                Log.e("test", "TFLite in Play Services failed to initialize.", it)
//            }
//    }

//    private fun signatureDetectLogic() {
//        // Initialize TFLite asynchronously
//        initializeTask
//            .addOnSuccessListener {
//                Log.i("test", "TFLite in Play Services initialized successfully.")
//                classifier = ImageClassificationHelper(this, MAX_REPORT, useGpu)
//                val embedding1 = classifier?.classify(face1, 0)
//            }
//
//    }

    private fun livenessCheckLogic() {
        getSelectedSteps()
        when (mutableStepList.isEmpty()) {
            true -> showToast(this, "You need to select at least one step")
            false ->
                livenessEntryPoint.startLiveness(
                    cameraSettings = CameraSettings(
                        livenessStepList = mutableStepList,
                        storageType = StorageType.INTERNAL
                    ),
                    context = this,
                ) { livenessCameraXResult ->
                    if (livenessCameraXResult.error == null) {
                        val listOfImages = arrayListOf<ByteArray>().apply {
                            livenessCameraXResult.createdBySteps?.let { photoResultList ->
                                this.addAll(
                                    photoResultList.map {
                                        Base64.decode(it.fileBase64, Base64.NO_WRAP)
                                    }
                                )
                            }
                        }

                        var byteArrayImage = listOfImages?.lastOrNull()
                        byteArrayImage?.let { byteArrayImage ->

                            Utility.showProgressD(this@OcrTestScreen)
                            lifecycleScope.launch(Dispatchers.IO) {
                                var cardImage = BitmapFactory.decodeByteArray(byteArrayImage, 0, byteArrayImage.size)
                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding?.faceLiveness?.setImageBitmap(cardImage)
                                }
                                var face2 = resultOcr?.faceImage

                                if (face2 == null) {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        Utility.hideProgressD(this@OcrTestScreen)
                                        Toast.makeText(this@OcrTestScreen, "Face not found in card scan, Please scan card again", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }
                                OcrModuleNew.extractFaceFromImage(cardImage) { face1 ->
                                    if (face1 == null) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            Utility.hideProgressD(this@OcrTestScreen)
                                            Toast.makeText(this@OcrTestScreen, "Face not found", Toast.LENGTH_SHORT).show()
                                        }
                                        return@extractFaceFromImage
                                    }
                                    OcrModuleNew.matchFaces(this@OcrTestScreen ,face1 ,face2) { scoreMatch ->
                                        lifecycleScope.launch(Dispatchers.Main) {
//                                    val tt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() -currentTime)
                                            if (scoreMatch > 40)
                                                binding?.faceMatchText?.text = "Face Matched"
                                            else
                                                binding?.faceMatchText?.text = "Face Not Matched"

                                            Log.i("test", " score matched ${scoreMatch}")
                                            Utility.hideProgressD(this@OcrTestScreen)
                                        }
                                    }
//                                    val embedding1 = classifier?.classify(face1, 0)
//                                    val embedding2 = classifier?.classify(face2, 0)
//
//                                    if (embedding1 != null && embedding2 != null) {
//                                        var scoreMatch = findCosineDistance(embedding1, embedding2)
//                                        lifecycleScope.launch(Dispatchers.Main) {
////                                    val tt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() -currentTime)
//                                            if (scoreMatch > 40)
//                                                binding?.faceMatchText?.text = "Face Matched"
//                                            else
//                                                binding?.faceMatchText?.text = "Face Not Matched"
////                                    binding?.processedText?.text = "Processed Time: ${tt} seconds"
//                                            Log.i("test", " score matched ${scoreMatch}")
//                                            Utility.hideProgressD(this@OcrTestScreen)
//                                        }
//                                    }
                                }
                            }


                        }
//                        imageListAdapter.imageList = listOfImages
//                        binding.tvListResult.text = getString(R.string.result_list, listOfImages.size.toString())
//                        binding.groupResultList.isVisible = true
                    } else {
                        livenessCameraXResult.error?.let {
                            Log.e(
                                this.localClassName,
                                it.toString()
                            )
                        }
                    }
                }
        }
    }

    private fun faceDetectLogic() {
        startActivity(Intent(this, FaceMatch::class.java))
    }

    private fun getSelectedSteps() {
        mutableStepList.clear()
        mutableStepList.add(StepLiveness.STEP_SMILE)
        mutableStepList.add(StepLiveness.STEP_BLINK)
        mutableStepList.add(StepLiveness.STEP_HEAD_LEFT)
        mutableStepList.add(StepLiveness.STEP_HEAD_RIGHT)
//        mutableStepList.add(StepLiveness.STEP_LUMINOSITY)
    }

    private fun selectTextAndDataLogic() {
        when {
            currentScanType == ScanType.Regula -> {
                var dataCLip = binding?.ocrTextRegula?.text.toString() + "\n" + binding?.ocrTextRegulaRaw?.text.toString()
                val clipboardManager: ClipboardManager? = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipData = ClipData.newPlainText("label", dataCLip)
                clipboardManager?.setPrimaryClip(clipData)
            }

            currentScanType == ScanType.OurOcr -> {
                var dataCLip = binding?.ocrText?.text.toString() + "\n" + binding?.ocrTextRaw?.text.toString()
                val clipboardManager: ClipboardManager? = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipData = ClipData.newPlainText("label", dataCLip)
                clipboardManager?.setPrimaryClip(clipData)

            }
        }
    }

    private fun showOurOcrLayoutLogic() {
        binding?.ocrTextLayout?.visibility = View.VISIBLE
        binding?.ocrTextRegulaLayout?.visibility = View.GONE
    }

    private fun showRegulaOcrLayoutLogic() {
        binding?.ocrTextLayout?.visibility = View.GONE
        binding?.ocrTextRegulaLayout?.visibility = View.VISIBLE
    }


    var resultOcr: BaseResult? = null
    private fun scanOcrLogic() {

        //            startActivity(Intent(this , OcrModule::class.java))
        Log.i("test", "sssssssssstartssssssss")
        Utility.showProgressD(this, "Loading Data")

//                Log.i("test", "now this happen")
        OcrModuleNew.checkIfNerRequirementsGood(lifecycleScope,this) {
            Utility.hideProgressD(this@OcrTestScreen)
            Log.i("test", "33333")

            OcrModuleNew.OcrModuleInit(this@OcrTestScreen) { result: BaseResult, cropedImage: Bitmap ->
                lifecycleScope.launch(Dispatchers.Main) {
                    resultOcr = result

                    currentScanType = ScanType.OurOcr

                    Utility.hideProgressD(this@OcrTestScreen)
                    ///  show next screen data
                    var jsonOb = JSONObject()
                    if (!result.name.isNullOrEmpty())
                        jsonOb.put("Name", "${result.name}")
                    if (!result.surname.isNullOrEmpty())
                        jsonOb.put("Sure Name", "${result.surname}")
                    if (!result.fullName.isNullOrEmpty())
                        jsonOb.put("Full Name", "${result.fullName}")
                    if (!result.idCard.isNullOrEmpty())
                        jsonOb.put("Id Card", "${result.idCard}")
                    if (!result.idNumber.isNullOrEmpty())
                        jsonOb.put("Id Number", "${result.idNumber}")
                    if (!result.validity.isNullOrEmpty())
                        jsonOb.put("Validity", "${result.validity}")
                    if (!result.issued.isNullOrEmpty())
                        jsonOb.put("Issued", "${result.issued}")
                    if (!result.licenseNumber.isNullOrEmpty())
                        jsonOb.put("License Number", "${result.licenseNumber}")
                    if (!result.dateOfBirth.isNullOrEmpty())
                        jsonOb.put("Date Of Birth", "${result.dateOfBirth}")
                    if (!result.sex.isNullOrEmpty())
                        jsonOb.put("Gender", "${result.sex}")
                    if (!result.bloodGroup.isNullOrEmpty())
                        jsonOb.put("Blood Group", "${result.bloodGroup}")
                    if (!result.nationality.isNullOrEmpty())
                        jsonOb.put("Nationality", "${result.nationality}")
                    if (!result.birthCountry.isNullOrEmpty())
                        jsonOb.put("Birth Country", "${result.birthCountry}")
                    if (!result.lastName.isNullOrEmpty())
                        jsonOb.put("Last Name", "${result.lastName}")
                    if (!result.villageOfOrigin.isNullOrEmpty())
                        jsonOb.put("Village Of Origin", "${result.villageOfOrigin}")
                    if (!result.placeOfBirth.isNullOrEmpty())
                        jsonOb.put("Place Of Birth", "${result.placeOfBirth}")
                    if (!result.dateOfIssue.isNullOrEmpty())
                        jsonOb.put("Date Of Issue", "${result.dateOfIssue}")
                    if (!result.dateOfExpiry.isNullOrEmpty())
                        jsonOb.put("Date Of Expiry", "${result.dateOfExpiry}")
                    if (!result.countryOfResidence.isNullOrEmpty())
                        jsonOb.put("Country Of Residence", "${result.countryOfResidence}")
                    if (!result.placeOfIssue.isNullOrEmpty())
                        jsonOb.put("Place Of Issue", "${result.placeOfIssue}")
                    if (!result.type.isNullOrEmpty())
                        jsonOb.put("TYPE", "${result.type}")
                    if (!result.passportNumber.isNullOrEmpty())
                        jsonOb.put("PASSPORT NO", "${result.passportNumber}")
                    if (!result.authority.isNullOrEmpty())
                        jsonOb.put("AUTHORITY", "${result.authority}")

                    Log.i("hello", "data holding ${result.mrzNumber}")

                    if (!result.mrzNumber.isNullOrEmpty())
                        jsonOb.put("MRZ NO", "${result.mrzNumber}")
                    if (!result.nationality.isNullOrEmpty())
                        jsonOb.put("NATIONALITY", "${result.nationality}")
                    if (!result.countryCode.isNullOrEmpty())
                        jsonOb.put("COUNTRY CODE", "${result.countryCode}")
                    if (!result.cardProbability.isNullOrEmpty())
                        jsonOb.put("Probability Card", "${result.cardProbability}")
                    if (!result.district.isNullOrEmpty())
                        jsonOb.put("District", "${result.district}")
                    if (!result.division.isNullOrEmpty())
                        jsonOb.put("Division", "${result.division}")
                    if (!result.location.isNullOrEmpty())
                        jsonOb.put("Location", "${result.location}")
                    if (!result.subLocation.isNullOrEmpty())
                        jsonOb.put("Sub Location", "${result.subLocation}")
                    if (!result.identifier.isNullOrEmpty())
                        jsonOb.put("identifier", "${result.identifier}")

                    binding?.ocrText?.text = jsonOb.toString(8)
                    binding?.ocrTextRaw?.text = result.frontCardData +"\n\n" + result.backCardData
                    binding?.cardImageFront?.setImageBitmap(result.cardFrontSideImage)
                    binding?.cardImageBack?.setImageBitmap(result.cardBackSideImage)
                    binding?.cardImageFace?.setImageBitmap(result.faceImage)
                    binding?.signatureCard?.setImageBitmap(result.signature?.firstOrNull())

                    Log.i("hello", "result  ${result}")

                }
            }
        }
        return


        val fm = supportFragmentManager
        val ft = fm?.beginTransaction()
        val prev = fm?.findFragmentByTag("SelectOcrTypeDialogue")
        if (prev != null) fm.beginTransaction()?.remove(prev)?.commit()
        val frag = SelectOcrTypeDialogue.newInstance({

        }, {
//            currentScanType = ScanType.Regula
//            scanCardLogic()
            // scan card and get document id and search from user if registered show ui if not move to registration screen
        })
        ft?.let { frag.show(it, "SelectOcrTypeDialogue") }
    }


}