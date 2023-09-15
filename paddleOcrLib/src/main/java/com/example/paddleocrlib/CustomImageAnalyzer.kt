package com.example.paddleocrlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleCoroutineScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.example.paddleocrlib.utils.ImageUtilsOld
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

var isProcessing: AtomicBoolean = AtomicBoolean(false)

class CustomImageAnalyzer(
    private val previewView: PreviewView?,
    private val context: Context,
    private val onFrameAnalyzed: (Bitmap) -> Unit,
    private val lifeCycleScope: LifecycleCoroutineScope,
    private val rectangleWidth: Int,
    private val rectangleHeight: Int,
    private val showMessage: (String) -> Unit,
    private val imageAnalyzed: (Bitmap) -> Unit,
    private val resultCallBack: (BaseResult,Bitmap) -> Unit,
) : ImageAnalysis.Analyzer {

    val pythonModPyt: PyObject? by lazy {
        Python.getInstance().getModule("SpacyModule")
    }


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {


//        previewView?.let { cropImageToPreviewView(imageProxy , it) }?.let {  }
        imageProxy.image?.let { image ->
            if (!isProcessing.get()) {
                lifeCycleScope.launch(Dispatchers.Main) {
                    showMessage("")
                }

//                val image = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)


//                val bitmap = imageToBitmap(image)
                val bitmap = cameraImageToBitmap(image)
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                lifeCycleScope.launch(Dispatchers.IO) {
                    isProcessing.set(true)

                    var rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                    rotatedBitmap = previewView?.width?.let { previewView.height.let { it1 -> cropBitmapWithAspectRatio(rotatedBitmap, it, it1) } } ?: return@launch
                    if (rectangleWidth != 0 && rectangleHeight != 0) {
                        rotatedBitmap = cropBitmapWithAspectRatio(rotatedBitmap, rectangleWidth, rectangleHeight)


                        var isBlury = ImageUtilsOld.isBlurry(rotatedBitmap, 20.0)
                        var hasGlare = false// ImageUtilsOld.hasGlare(rotatedBitmap)
//                        var isCardDetected = ImageUtils.isCardDetected(context, rotatedBitmap)
                        lifeCycleScope.launch(Dispatchers.Main) {
//                            showMessage("Is Card Detected ${isCardDetected}")
//                            Log.i("test" ,"Is Card Detected ${isCardDetected}")
                            if (isBlury) {
                                showMessage("Image is blurry")
                            } else if (hasGlare) {
                                showMessage("Image has glare")
                            }
                        }
                        if (isBlury || hasGlare) {
                            try {
                                delay(1000)
                            } catch (e: Exception) {
                            }
                            isProcessing.set(false)
                            return@launch
                        }
                        lifeCycleScope.launch(Dispatchers.Main) {
                            showMessage("Document is processing")
                        }
                        rotatedBitmap = ImageUtilsOld.upscaleImageWithFixedWidth(rotatedBitmap, 1000)

//                        getOcr({ ocrText ->
//                            Log.i("hello", "all text ${ocrText}")
////                            isProcessing.set(false)
////                            return@getOcr
//                            if(ocrText.isEmpty()){
//                                isProcessing.set(false)
//                                return@getOcr
//                            }
//
//                            var result = processData(ocrText, context)
//                            result?.cardImage = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
//                            Log.i("hello", "name  ${result?.name}")
//                            if (result?.name.isNullOrEmpty() ) {
//                                isProcessing.set(false)
//                                return@getOcr
//                            } else {
//                                Log.i("hello" ,"else called")
//                                result?.let {
//                                    resultCallBack.invoke(it ,rotatedBitmap)
//                                }
//                            }
//
//                        }, rotatedBitmap)


                        imageAnalyzed.invoke(rotatedBitmap)

//                        var isBlury = ImageUtils.isBluryImage(context, rotatedBitmap)
//                        if (isBlury) {
//                            showMessage.invoke("Image is Blurry")
//                        }
//                        rotatedBitmap = ImageUtils.straightenLines(rotatedBitmap)
//                    rotatedBitmap = ImageUtils.gaussianBlur(context,rotatedBitmap ,5f)
//                        rotatedBitmap = ImageUtils.enhanceLocalContrast(context, rotatedBitmap)

                        onFrameAnalyzed(rotatedBitmap)

//                     ImageUtils.isCardDetected(context,rotatedBitmap )
                    }
                }
            }
        }
        imageProxy.close()
    }

//    private fun getOcr(ocrText: (String) -> Unit, bitmapOriginal: Bitmap) {
//        val bitmap = bitmapOriginal.copy(bitmapOriginal.config, true)
//        //using new
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        val image = InputImage.fromBitmap(bitmap, 0)
//        val result = recognizer.process(image)
//            .addOnSuccessListener { visionText ->
//                // Task completed successfully
//                var allLines: String = ""
//                var shouldBreakLoop = false
//                for (block in visionText.textBlocks) {
//                    for (line in block.lines) {
//                        if (line.confidence < 0.30) {
//                            allLines = ""
//                            shouldBreakLoop = true
//                        }
//                        if(shouldBreakLoop) {
//                            break
//                        }
////                        Log.i("hello", "line.confidence   ${line.confidence}")
//                        allLines = allLines + " " + line.text
//                    }
//                    if(shouldBreakLoop) {
//                        break
//                    }
//                }
//                ocrText.invoke(allLines)
//            }
//            .addOnFailureListener { e ->
//                // Task failed with an exception
//                // ...
//            }
//    }


    fun cropImageCenter(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        // Calculate the current aspect ratio
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

        // Calculate the crop size based on the target aspect ratio
        val cropWidth: Int
        val cropHeight: Int
        if (sourceRatio > targetRatio) {
            cropWidth = (sourceHeight * targetRatio).toInt()
            cropHeight = sourceHeight
        } else {
            cropWidth = sourceWidth
            cropHeight = (sourceWidth / targetRatio).toInt()
        }

        // Calculate the coordinates of the top-left corner of the crop region
        val cropLeft = (sourceWidth - cropWidth) / 2
        val cropTop = (sourceHeight - cropHeight) / 2

        // Crop the image to the target aspect ratio
        val cropped = Bitmap.createBitmap(source, cropLeft, cropTop, cropWidth, cropHeight)

        // Scale the cropped image to the target size
        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)

        return scaled
    }

    fun cropBitmapWithAspectRatio(originalBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        // Calculate the target aspect ratio
        val targetAspectRatio = targetWidth.toDouble() / targetHeight.toDouble()

        // Get the original bitmap dimensions
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val originalAspectRatio = originalWidth.toDouble() / originalHeight.toDouble()

        // Calculate the new dimensions based on the target aspect ratio
        val newWidth: Int
        val newHeight: Int
        if (originalAspectRatio > targetAspectRatio) {
            // Crop the width to maintain the aspect ratio
            newWidth = (originalHeight * targetAspectRatio).toInt()
            newHeight = originalHeight
        } else {
            // Crop the height to maintain the aspect ratio
            newWidth = originalWidth
            newHeight = (originalWidth / targetAspectRatio).toInt()
        }

        // Calculate the cropping positions to center the new bitmap
        val startX = (originalWidth - newWidth) / 2
        val startY = (originalHeight - newHeight) / 2

        // Crop the original bitmap with the calculated dimensions
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, startX, startY, newWidth, newHeight)

        // Scale the cropped bitmap to the desired target dimensions
        return Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
    }


    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val yuvData = out.toByteArray()

        return BitmapFactory.decodeByteArray(yuvData, 0, yuvData.size)
    }

    fun cameraImageToBitmap(cameraImage: Image): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(cameraImage.width, cameraImage.height, Bitmap.Config.ARGB_8888)
        cameraImage.use { bitmapBuffer.copyPixelsFromBuffer(cameraImage.planes[0].buffer) }
        return bitmapBuffer
    }


    fun processData(lineTest: String, context: Context): BaseResult? {
        var resultObje: BaseResult? = BaseResult()
        resultObje?.rawLine = lineTest
        var modelPathCard = MyPrefManager.getInstance(context!!)?.modelAllCardPath ?: ""
        val processedText = try {
//            val py = Python.getInstance()

//            Log.i("test", "line text at start   ${lineTest}")

            var result: PyObject? = pythonModPyt?.callAttr("detect_text_classify", lineTest, modelPathCard)
//            Log.i("hello", "processedText  ${result?.toJava(String::class.java)}")
            result?.toJava(String::class.java) ?: "{}"
        } catch (e: java.lang.Exception) {
            Log.i("test", "error ", e)
            ""
        }
//        Log.i("test", "processedText at start   ${processedText}")

        var cardList: MutableList<CardTypeCountry?> = arrayListOf()
        var cardObj = try {
            JSONObject(processedText)
        } catch (e: java.lang.Exception) {
            null
        }

        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("sa_passport", 0.0) ?: 0.0, cardType = CardType.Passport.name, countryCard = CountryCard.SouthAfrica.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("zim_passport", 0.0) ?: 0.0, cardType = CardType.Passport.name, countryCard = CountryCard.Zimbabwe.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_passport", 0.0) ?: 0.0, cardType = CardType.Passport.name, countryCard = CountryCard.Kenya.name))

        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("sa_id_card", 0.0) ?: 0.0, cardType = CardType.IDCard.name, countryCard = CountryCard.SouthAfrica.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("sa_license", 0.0) ?: 0.0, cardType = CardType.License.name, countryCard = CountryCard.SouthAfrica.name))

        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_id_card", 0.0) ?: 0.0, cardType = CardType.IDCard.name, countryCard = CountryCard.Kenya.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_license", 0.0) ?: 0.0, cardType = CardType.License.name, countryCard = CountryCard.Kenya.name))

        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("zim_license", 0.0) ?: 0.0, cardType = CardType.License.name, countryCard = CountryCard.Zimbabwe.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("zim_id_card", 0.0) ?: 0.0, cardType = CardType.IDCard.name, countryCard = CountryCard.Zimbabwe.name))

        var selectedItem: CardTypeCountry? = null
        var modelPath = ""
        var probability = 0.0
        cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {
            resultObje?.showDataCard = "${it?.cardType} - ${it?.countryCard} - ${it?.valueItem}"
        }
//                resultObje?.cardProbability
        cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {
            if (selectedItem != null) {
                Log.i("hello", "Selected  ${selectedItem?.countryCard} / ${selectedItem?.cardType} / ${selectedItem?.valueItem}")
            }

            if ((it.valueItem) >= 0.80) {
                probability = it.valueItem
                selectedItem = it
//                Log.i("hello", "Selected  ${it}")


                when {
                    selectedItem?.countryCard == CountryCard.SouthAfrica.name -> {
                        when (selectedItem?.cardType) {
                            CardType.Passport.name -> {
                                modelPath = MyPrefManager.getInstance(context)?.modelSaPassportPath ?: ""
                            }

                            CardType.License.name -> {
                                modelPath = MyPrefManager.getInstance(context)?.modelSaLicensePath ?: ""
                            }

                            CardType.IDCard.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelSaIdCardPath ?: ""
                            }
                        }
                    }

                    selectedItem?.countryCard == CountryCard.Zimbabwe.name -> {
                        when (selectedItem?.cardType) {
                            CardType.Passport.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabwePassportPath ?: ""
                            }

                            CardType.License.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabweLicenseCardPath ?: ""
                            }

                            CardType.IDCard.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabweIdCardPath ?: ""
                            }
                        }
                    }

                    selectedItem?.countryCard == CountryCard.Kenya.name -> {
                        when (selectedItem?.cardType) {
                            CardType.Passport.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaPassportPath ?: ""
                            }

                            CardType.License.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaLicensePath ?: ""
                            }

                            CardType.IDCard.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaIdCardFrontPath ?: ""
                            }
                        }
                    }
                }

                Log.i("test", "model path if  ${modelPath}")

            } else {
                Log.i("test", "model path values   ${it.valueItem}")
                Log.i("test", "model path not selected   ${modelPath}")
                selectedItem = null
            }


        }

        var processedTextNer = "{}"
        if (modelPath.isEmpty() == false) {
            processedTextNer = try {
                val result: PyObject? = pythonModPyt?.callAttr("process_text", lineTest.trim(), modelPath)
                result?.toJava(String::class.java) ?: "{}"
            } catch (e: java.lang.Exception) {
                Log.i("test", "error ", e)
                "{}"
            }
//            Log.i("hello", "processedText ner from model ${processedTextNer}")
        } else {
            var hypoThesis = lineTest.trim().replace(" ", "")
            if (hypoThesis.contains("REPUBLICOFZIMBABWE", true) && hypoThesis.contains("NATIONALREGISTRATION", true)) {
                if (selectedItem == null)
                    selectedItem = CardTypeCountry()
                selectedItem?.countryCard = CountryCard.Zimbabwe.name
                selectedItem?.cardType = CardType.IDCard.name
                selectedItem?.valueItem = 100.0
                modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabweIdCardPath ?: ""
            } else if (hypoThesis.contains("REPUBLIC OF SOUTHAFRICA", true) && hypoThesis.contains("Passport/Passeport", true)) {
                if (selectedItem == null)
                    selectedItem = CardTypeCountry()
                selectedItem?.countryCard = CountryCard.SouthAfrica.name
                selectedItem?.cardType = CardType.Passport.name
                selectedItem?.valueItem = 100.0
                modelPath = MyPrefManager.getInstance(context)?.modelSaPassportPath ?: ""
            }
//                    REPUBLIC OFZIMBABWE NATIONALREGISTRATION 24-092622G24CITF ID NUMBER SURNAME TADYANEMHANDU MARIA FIRST NAME 26/06/1976 DATE OF BRTH VILLAGE OFORIGIN CHINHOYI PLACE OF BIRTH 22/06/2009 DATE OF ISSU Signature of Holder
        }

        val mrzData = try {
            JSONObject(processedTextNer.trim())
        } catch (e: java.lang.Exception) {
            Log.i("test", "MRZ data exception ${e} + ")
            JSONObject()
        }
        Log.i("hello", "Object data from model ${mrzData}")
        if (selectedItem != null) {
            Log.i("test", "Selected not null   ${selectedItem?.cardType}")

            Log.i("test", "Selected when elsee   ${selectedItem?.cardType}")
            when {
                selectedItem?.countryCard == CountryCard.SouthAfrica.name -> {
                    Log.i("test", "Selected when   ${selectedItem?.cardType}")

                    when (selectedItem?.cardType) {
                        CardType.Passport.name -> {

                            resultObje?.mrzNumber = mrzData.optString("MRZ NO").replace(" ", "")
                            resultObje?.name = mrzData.optString("NAME")
                            resultObje?.type = mrzData.optString("TYPE")
                            resultObje?.country = mrzData.optString("COUNTRY CODE")
                            resultObje?.passportNumber = mrzData.optString("PASSPORT NO")
                            resultObje?.idNumber = mrzData.optString("ID NO")
                            resultObje?.surname = mrzData.optString("SURNAME")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH")
                            resultObje?.sex = mrzData.optString("SEX")
                            resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH")
                            resultObje?.dateOfExpiry = mrzData.optString("DATE OF EXPIRY")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE")
                            resultObje?.authority = mrzData.optString("AUTHORITY")
                            resultObje?.nationality = mrzData.optString("NATIONALITY")
                            resultObje?.countryCode = mrzData.optString("COUNTRY CODE")

                        }

                        CardType.License.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false
                            resultObje?.name = mrzData.optString("NAME", "")
                            resultObje?.sex = mrzData.optString("SEX", "")
                            resultObje?.idNumber = mrzData.optString("ID NO", "")
                            resultObje?.validity = mrzData.optString("VALIDITY", "")
                            resultObje?.issued = mrzData.optString("ISSUED", "")
                            resultObje?.dateOfExpiry = mrzData.optString("DATE OF EXPIRY", "")
                            resultObje?.licenseNumber = mrzData.optString("LIC NO", "")

                        }

                        CardType.IDCard.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false

                            resultObje?.name = mrzData.optString("NAME", "")
                            resultObje?.surname = mrzData.optString("SURNAME", "")

                            resultObje?.sex = mrzData.optString("SEX", "")
                            resultObje?.nationality = mrzData.optString("NATIONALITY", "")
                            resultObje?.idNumber = mrzData.optString("ID NO", "")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH", "")
                            resultObje?.birthCountry = mrzData.optString("COUNTRY OF BIRTH", "")
                            resultObje?.status = mrzData.optString("STATUS", "")


                            //{"SA_IDCARD": "NATIONALIDENTITY CARD", "NAMES": "SUMSUB", "SEX": "M", "NATIONALITY": "RSA", "ID_NO": "0123456789012",
                            // "DATE OF BIRTH": "22JUL1980", "COUNTRY OF BIRTH": "RSA", "STATUS": "sumsub CITIZEN"}
                        }
                    }
                }

                selectedItem?.countryCard == CountryCard.Zimbabwe.name -> {
                    when (selectedItem?.cardType) {

                        CardType.Passport.name -> {

                            resultObje?.mrzNumber = mrzData.optString("MRZ NO").replace(" ", "")
                            resultObje?.name = mrzData.optString("NAME")
                            resultObje?.type = mrzData.optString("TYPE")
                            resultObje?.country = mrzData.optString("COUNTRY CODE")
                            resultObje?.passportNumber = mrzData.optString("PASSPORT NO")
                            resultObje?.idNumber = mrzData.optString("ID NO")
                            resultObje?.surname = mrzData.optString("SURNAME")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH")
                            resultObje?.sex = mrzData.optString("SEX")
                            resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH")
                            resultObje?.dateOfExpiry = mrzData.optString("DATE OF EXPIRY")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE")
                            resultObje?.authority = mrzData.optString("AUTHORITY")
                            resultObje?.nationality = mrzData.optString("NATIONALITY")
                            resultObje?.countryCode = mrzData.optString("COUNTRY CODE")

                        }

                        CardType.License.name -> {
                            Log.i("test", "Selected when lincence   ${selectedItem?.cardType.equals(CardType.License.name)}")
                            Log.i("test", "Selected when IDcard   ${selectedItem?.cardType.equals(CardType.IDCard.name)}")

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false

                            resultObje?.name = mrzData.optString("NAME", "")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH", "")
                            resultObje?.idNumber = mrzData.optString("ID NO", "")
                            resultObje?.licenseNumber = mrzData.optString("LIC NO", "")

                        }

                        CardType.IDCard.name -> {
                            Log.i("test", "Selected when IDcard 2  ${selectedItem?.cardType.equals(CardType.IDCard.name)}")
                            Log.i("test", "Selected when lincence 2  ${selectedItem?.cardType.equals(CardType.License.name)}")

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false

                            resultObje?.idCard = mrzData.optString("ID CARD", "")
                            resultObje?.idNumber = mrzData.optString("ID NO", "")
                            resultObje?.surname = mrzData.optString("SURNAME", "")
                            resultObje?.name = mrzData.optString("NAME", "")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH", "")
                            resultObje?.villageOfOrigin = mrzData.optString("VILLAGE OF ORIGIN", "")
                            resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH", "")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE", "")

                        }
                    }
                }

                selectedItem?.countryCard == CountryCard.Kenya.name -> {
                    when (selectedItem?.cardType) {

                        CardType.Passport.name -> {

                            resultObje?.mrzNumber = mrzData.optString("MRZ NO").replace(" ", "")
                            resultObje?.name = mrzData.optString("NAME")
                            resultObje?.type = mrzData.optString("TYPE")
                            resultObje?.country = mrzData.optString("COUNTRY CODE")
                            resultObje?.passportNumber = mrzData.optString("PASSPORT NO")
                            resultObje?.idNumber = mrzData.optString("ID NO")
                            resultObje?.surname = mrzData.optString("SURNAME")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH")
                            resultObje?.sex = mrzData.optString("SEX")
                            resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH")
                            resultObje?.dateOfExpiry = mrzData.optString("DATE OF EXPIRY")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE")
                            resultObje?.authority = mrzData.optString("AUTHORITY")
                            resultObje?.nationality = mrzData.optString("NATIONALITY")
                            resultObje?.countryCode = mrzData.optString("COUNTRY CODE")

                        }

                        CardType.License.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false

                            resultObje?.surname = mrzData.optString("SURNAME", "")
                            resultObje?.name = mrzData.optString("NAME", "")
                            resultObje?.idNumber = mrzData.optString("ID NO", "")
                            resultObje?.licenseNumber = mrzData.optString("LIC NO", "")
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH", "")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE", "")
                            resultObje?.dateOfExpiry = mrzData.optString("DATE OF EXPIRY", "")
                            resultObje?.sex = mrzData.optString("SEX", "")
                            resultObje?.bloodGroup = mrzData.optString("BLOOD GROUP", "")
                            resultObje?.countryOfResidence = mrzData.optString("COUNTRY OF RESIDENCE", "")

                        }

                        CardType.IDCard.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false
                            resultObje?.idNumber = mrzData.getString("ID NO")
                            resultObje?.fullName = mrzData.getString("FULL NAME")
                            resultObje?.dateOfBirth = mrzData.getString("DATE OF BIRTH")
                            resultObje?.sex = mrzData.getString("SEX")
                            resultObje?.placeOfBirth = mrzData.getString("PLACE OF BIRTH")
                            resultObje?.placeOfIssue = mrzData.getString("PLACE OF ISSUE")
                            resultObje?.dateOfIssue = mrzData.getString("DATE OF ISSUE")
                        }
                    }
                }
            }
        } else {
            Log.i("test", "Selected isss null   ${selectedItem?.cardType}")

        }

//                {"passport": 0.9925427436828613, "sa_id_card": 0.003589725587517023, "kenya_id_card": 0.0038675221148878336}


//                return null

        resultObje?.cardProbability = probability.toString()



//        resultObje?.cardImage = originPicBitmap?.copy(Bitmap.Config.ARGB_8888, true)
//        picBitmap = originPicBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        return resultObje
    }

    enum class CountryCard {
        Zimbabwe, SouthAfrica, Kenya, None
    }

    enum class CardType {
        License, IDCard, Passport
    }

    data class CardTypeCountry(
        var cardType: String = "",
        var countryCard: String = "",
        var valueItem: Double = 0.0,
    )

}



