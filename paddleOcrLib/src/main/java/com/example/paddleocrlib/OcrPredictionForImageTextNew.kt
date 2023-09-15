package com.example.paddleocrlib

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.util.Consumer
import com.baidu.paddle.fastdeploy.pipeline.PPOCRv3
import com.baidu.paddle.fastdeploy.vision.OCRResult
import com.baidu.paddle.fastdeploy.RuntimeOption
import com.baidu.paddle.fastdeploy.vision.ocr.Classifier
import com.baidu.paddle.fastdeploy.vision.ocr.DBDetector
import com.baidu.paddle.fastdeploy.vision.ocr.Recognizer
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import org.json.JSONObject
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class OcrPredictionForImageTextNew(bitmap: Bitmap?, var context: Context, var confidencce: Float, var errorHandler: Consumer<BaseResult?>) {

    init {
        Log.i("test", "init function ")
        if (isProcessingString.get() == false) {
            isProcessingString.set(true)
            if (predictor == null)
                predictor = PPOCRv3()
            if (result == null)
                result = OCRResult()

            if (pythonModPyt == null) {
                val py = Python.getInstance()
                pythonModPyt = py.getModule("SpacyModule")
            }
            Log.d("test", "result obj initialized  = ${result?.initialized()}")
            resultObje = BaseResult()
            if (checkAndUpdateSettings(context) == true) {
                Log.i("test", "checkAndUpdateSettings is trueee")

                resultNum = confidencce
                picBitmap = bitmap //Utils.decodeBitmap(path, 1920, 1080);
                originPicBitmap = picBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                result = predictor?.predict(picBitmap, true)
                val obj = detail(context)
                obj?.cardImage = bitmap
                errorHandler.accept(detail(context))
            } else {
                Handler(Looper.getMainLooper()).post { Toast.makeText(context, " Predictor is not initialized ", Toast.LENGTH_SHORT).show() }
            }
            isProcessingString.set(false)
        } else {
            Log.i("test", "isProcessingString is trueee")
        }
    }

    fun processImage(bitmap: Bitmap) {
        var baseResult = BaseResult()
        if (predictor?.initialized() == true && result?.initialized() == true) {
//            Log.i("test", "checkAndUpdateSettings is trueee")
            resultNum = confidencce
            picBitmap = bitmap //Utils.decodeBitmap(path, 1920, 1080);
            originPicBitmap = picBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            result = predictor?.predict(picBitmap, true)
            val obj = detailNew(context, baseResult)
            obj?.cardImage = bitmap
            errorHandler.accept(detail(context))
        } else {
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, " Predictor is not initialized ", Toast.LENGTH_SHORT).show() }
        }
    }

    fun checkAndUpdateSettings(context: Context): Boolean? {
        modelDir = context.getString(R.string.OCR_MODEL_DIR_DEFAULT)
        labelPath = context.getString(R.string.OCR_REC_LABEL_DEFAULT)
        cpuThreadNum = context.getString(R.string.CPU_THREAD_NUM_DEFAULT).toInt()
        cpuPowerMode = context.getString(R.string.CPU_POWER_MODE_DEFAULT)
        enableLiteFp16 = "true"
        scoreThreshold = context.getString(R.string.SCORE_THRESHOLD_DEFAULT).toFloat()
        val realModelDir = context.cacheDir.toString() + "/" + modelDir
        val detModelName = "ch_PP-OCRv3_det_infer"
        val clsModelName = "ch_ppocr_mobile_v2.0_cls_infer"
        val recModelName = "ch_PP-OCRv3_rec_infer"
        val realDetModelDir = "$realModelDir/$detModelName"
        val realClsModelDir = "$realModelDir/$clsModelName"
        val realRecModelDir = "$realModelDir/$recModelName"
        val srcDetModelDir = "$modelDir/$detModelName"
        val srcClsModelDir = "$modelDir/$clsModelName"
        val srcRecModelDir = "$modelDir/$recModelName"
        Utils.copyDirectoryFromAssets(context, srcDetModelDir, realDetModelDir)
        Utils.copyDirectoryFromAssets(context, srcClsModelDir, realClsModelDir)
        Utils.copyDirectoryFromAssets(context, srcRecModelDir, realRecModelDir)
        val realLabelPath = context.cacheDir.toString() + "/" + labelPath
        Utils.copyFileFromAssets(context, labelPath, realLabelPath)
        val detModelFile = "$realDetModelDir/inference.pdmodel"
        val detParamsFile = "$realDetModelDir/inference.pdiparams"
        val clsModelFile = "$realClsModelDir/inference.pdmodel"
        val clsParamsFile = "$realClsModelDir/inference.pdiparams"
        val recModelFile = "$realRecModelDir/inference.pdmodel"
        val recParamsFile = "$realRecModelDir/inference.pdiparams"
        val detOption = RuntimeOption()
        val clsOption = RuntimeOption()
        val recOption = RuntimeOption()
        detOption.setCpuThreadNum(cpuThreadNum)
        clsOption.setCpuThreadNum(cpuThreadNum)
        recOption.setCpuThreadNum(cpuThreadNum)
        detOption.setLitePowerMode(cpuPowerMode)
        clsOption.setLitePowerMode(cpuPowerMode)
        recOption.setLitePowerMode(cpuPowerMode)
        if (java.lang.Boolean.parseBoolean(enableLiteFp16)) {
            detOption.enableLiteFp16()
            clsOption.enableLiteFp16()
            recOption.enableLiteFp16()
        }
        val detModel = DBDetector(detModelFile, detParamsFile, detOption)
        val clsModel = Classifier(clsModelFile, clsParamsFile, clsOption)
        val recModel = Recognizer(recModelFile, recParamsFile, realLabelPath, recOption)
        predictor?.init(detModel, clsModel, recModel)
        return predictor?.initialized()
    }

    enum class CountryCard {
        Zimbabwe, SouthAfrica, Kenya, None
    }

    enum class CardType {
        License, IDCardFront, IDCardBack, Passport
    }

    data class CardTypeCountry(
        var cardType: String = "",
        var countryCard: String = "",
        var valueItem: Double = 0.0,
    )


    fun detail(context: Context?): BaseResult? {
        Log.i("test", "detail function ")
        return if (result?.initialized() == true) {
            if (result == null) {
                Log.i("test", "result is null ")

                return null
            }
            Log.i("test", "result is not null ")


//            SystemClock.sleep((TIME_SLEEP_INTERVAL * 10).toLong())
            if (result?.initialized() == true) {
                texts = result?.mText ?: arrayOf()
                recScores = result?.mRecScores ?: floatArrayOf()
                var boxesWords = result?.mBoxes

                var line = ""
                var lineTest = ""
                initialized = result?.initialized() ?: false
                if (initialized) {
                    for (i in texts.indices) {
                        if (recScores[i] > resultNum) {
                            line = """$line${texts[i]}"""
                            lineTest = """$lineTest ${texts[i]}"""
//                            Log.i("test" ,"boxesWords[$i]  ${boxesWords[i][1]}  text data => ${texts[i]}")
                            results.add(BaseResultModel(i + 1, texts[i], recScores[i], boxesWords!![i]))
                        }
                    }
                }
                Log.i("hello", "lineTest  ${lineTest}")

                resultObje?.rawLine = lineTest

//                val processedText = try {
//                    val py = Python.getInstance()
//                    var pythonMod = py.getModule("SpacyModule")
//                    var result: PyObject = pythonMod.callAttr("process_text", lineTest, MyPrefManager.getInstance(context!!)?.mrzModelPath)
//                    Log.i("test", "processedText  ${result.toJava(String::class.java)}")
//                } catch (e: Exception) {
//                    Log.i("test", "error ", e)
//                }

                /*
                *  check type of card
                *
                *
                * */


                var modelPathCard = MyPrefManager.getInstance(context!!)?.modelAllCardPath ?: ""
                val processedText = try {
                    val py = Python.getInstance()

                    Log.i("hello", "line text at start   ${lineTest}")

                    var result: PyObject? = pythonModPyt?.callAttr("detect_text_classify", lineTest, modelPathCard)
//                    Log.i("test", "processedText  ${result.toJava(String::class.java)}")
                    result?.toJava(String::class.java) ?: "{}"
                } catch (e: Exception) {
                    Log.i("test", "error ", e)
                    ""
                }
                Log.i("test", "processedText at start   ${processedText}")

                var cardList: MutableList<CardTypeCountry?> = arrayListOf()
                var cardObj = try {
                    JSONObject(processedText)
                } catch (e: Exception) {
                    null
                }

                cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_id_front", 0.0) ?: 0.0, cardType = CardType.IDCardFront.name, countryCard = CountryCard.Kenya.name))
                cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_id_back", 0.0) ?: 0.0, cardType = CardType.IDCardBack.name, countryCard = CountryCard.Kenya.name))


                var selectedItem: CardTypeCountry? = null
                var modelPath = ""
                var probability = 0.0
                cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {
                    resultObje?.showDataCard = "${it?.cardType} - ${it?.countryCard} - ${it.valueItem}"
                }
//                resultObje?.cardProbability
                cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {

                    if ((it.valueItem) >= 0.70) {
                        probability = it.valueItem
                        selectedItem = it
                        Log.i("test", "Selected  ${it}")
                        Log.i("test", "Selected  ${selectedItem!!.cardType}")

                        when {
                            selectedItem?.countryCard == CountryCard.Kenya.name -> {
                                when (selectedItem?.cardType) {

                                    CardType.IDCardFront.name -> {
                                        modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaIdCardFrontPath ?: ""
                                    }

                                    CardType.IDCardBack.name -> {
                                        modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaIdCardBackPath ?: ""
                                    }
                                }
                            }
                        }

                        Log.i("test", "model path if  ${modelPath}")

                    } else {
//                        Log.i("test", "model path values   ${it.valueItem}")
//                        Log.i("test", "model path not selected   ${modelPath}")
                        selectedItem = null
                    }


                }

                var processedTextNer = "{}"
                if (modelPath.isEmpty() == false) {
                    processedTextNer = try {
                        val result: PyObject? = pythonModPyt?.callAttr("process_text", lineTest.trim(), modelPath)
//                        Log.i("test", "processedText ner  ${result.toJava(String::class.java)}")
//                        Log.i("test", "result  = ${result.toJava(String::class.java)}")
                        result?.toJava(String::class.java) ?: "{}"
                    } catch (e: Exception) {
                        Log.i("test", "error ", e)
                        "{}"
                    }
                    Log.i("hello", "processedText ner ${processedTextNer}")
                } else {
                    var hypoThesis = lineTest.trim().replace(" ", "")
                    if (hypoThesis.contains("REPUBLICOFZIMBABWE", true) && hypoThesis.contains("NATIONALREGISTRATION", true)) {
                        if (selectedItem == null)
                            selectedItem = CardTypeCountry()
                        selectedItem?.countryCard = CountryCard.Zimbabwe.name
                        selectedItem?.cardType = CardType.IDCardFront.name
                        selectedItem?.valueItem = 100.0
                        modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabweIdCardPath ?: ""
                    } else if (hypoThesis.contains("REPUBLIC OF SOUTHAFRICA", true) && hypoThesis.contains("Passport/Passeport", true)) {
                        if (selectedItem == null)
                            selectedItem = CardTypeCountry()
                        selectedItem?.countryCard = CountryCard.SouthAfrica.name
                        selectedItem?.cardType = CardType.Passport.name
                        selectedItem?.valueItem = 100.0
                        modelPath = MyPrefManager.getInstance(context!!)?.modelSaPassportPath ?: ""
                    }
//                    REPUBLIC OFZIMBABWE NATIONALREGISTRATION 24-092622G24CITF ID NUMBER SURNAME TADYANEMHANDU MARIA FIRST NAME 26/06/1976 DATE OF BRTH VILLAGE OFORIGIN CHINHOYI PLACE OF BIRTH 22/06/2009 DATE OF ISSU Signature of Holder
                }

                val mrzData = try {
                    JSONObject(processedTextNer.trim())
                } catch (e: Exception) {
                    Log.i("test", "MRZ data exception ${e} + ")
                    JSONObject()
                }
                Log.i("hello", "Data ${mrzData}")
                if (selectedItem != null) {
                    Log.i("hello", "Selected not null   ${selectedItem?.cardType}")

                    Log.i("test", "Selected when elsee   ${selectedItem?.cardType}")
                    when {

                        selectedItem?.countryCard == CountryCard.Kenya.name -> {
                            when (selectedItem?.cardType) {
                                CardType.IDCardFront.name -> {

                                    resultObje?.cardType = selectedItem?.cardType ?: ""
                                    resultObje?.country = selectedItem?.countryCard ?: ""
                                    resultObje?.isPassport = false
                                    resultObje?.idNumber = mrzData.optString("ID NO" ,"")
                                    resultObje?.name = mrzData.optString("NAME" ,"")
//                                    resultObje?.fullName = mrzData.getString("FULL NAME") //
                                    resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH" ,"")
                                    resultObje?.sex = mrzData.optString("SEX")
                                    resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH" ,"")
                                    resultObje?.placeOfIssue = mrzData.optString("PLACE OF ISSUE","")
                                    resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE" ,"")
                                }

                                CardType.IDCardBack.name -> {

                                    resultObje?.cardType = selectedItem?.cardType ?: ""
                                    resultObje?.country = selectedItem?.countryCard ?: ""
                                    resultObje?.isPassport = false

                                    resultObje?.district = mrzData.optString("DISTRICT" ,"")
                                    resultObje?.division = mrzData.optString("DIVISION" ,"")
                                    resultObje?.location = mrzData.optString("LOCATION","")
                                    resultObje?.subLocation = mrzData.optString("SUB-LOCATION" ,"")
                                    resultObje?.identifier = mrzData.optString("IDENTIFIER" ,"")
                                    resultObje?.mrzNumber = mrzData.optString("MRZ","")
//                                    resultObje?.dateOfIssue = mrzData.getString("DATE OF ISSUE")
                                }
                            }
                        }
                    }
                } else {
                    Log.i("hello", "Selected isss null   ${selectedItem?.cardType}")
                }
                resultObje?.cardProbability = probability.toString()
                picBitmap = originPicBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                resultObje
            } else {
                Handler(Looper.getMainLooper()).post { Toast.makeText(context, "Predictor not initialized", Toast.LENGTH_SHORT).show() }
                null
            }
        } else {
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, " Result is null ", Toast.LENGTH_SHORT).show() }
            null
        }
    }


    fun detailNew(context: Context?, resultObje: BaseResult): BaseResult? {

        Log.i("test", "result is not null ")

        var texts = result?.mText ?: arrayOf()
        var recScores = result?.mRecScores ?: floatArrayOf()
        var boxesWords = result?.mBoxes

        var line = ""
        var lineTest = ""
        initialized = result?.initialized() ?: false
        if (initialized) {
            for (i in texts.indices) {
                if (recScores[i] > resultNum) {
                    line = """$line${texts[i]}"""
                    lineTest = """$lineTest ${texts[i]}"""
//                            Log.i("test" ,"boxesWords[$i]  ${boxesWords[i][1]}  text data => ${texts[i]}")
                    results.add(BaseResultModel(i + 1, texts[i], recScores[i], boxesWords!![i]))
                }
            }
        }
        Log.i("test", "lineTest  ${lineTest}")

        resultObje?.rawLine = lineTest

        var modelPathCard = MyPrefManager.getInstance(context!!)?.modelAllCardPath ?: ""
        val processedText = try {
            Log.i("test", "line text at start   ${lineTest}")
            var result: PyObject? = pythonModPyt?.callAttr("detect_text_classify", lineTest, modelPathCard)
//                    Log.i("test", "processedText  ${result.toJava(String::class.java)}")
            result?.toJava(String::class.java) ?: "{}"
        } catch (e: Exception) {
            Log.i("test", "error ", e)
            ""
        }
        Log.i("test", "processedText at start   ${processedText}")

        var cardList: MutableList<CardTypeCountry?> = arrayListOf()
        var cardObj = try {
            JSONObject(processedText)
        } catch (e: Exception) {
            null
        }

        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_id_front", 0.0) ?: 0.0, cardType = CardType.IDCardFront.name, countryCard = CountryCard.Kenya.name))
        cardList.add(CardTypeCountry(valueItem = cardObj?.optDouble("kenya_id_back", 0.0) ?: 0.0, cardType = CardType.IDCardBack.name, countryCard = CountryCard.Kenya.name))


        var selectedItem: CardTypeCountry? = null
        var modelPath = ""
        var probability = 0.0
        cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {
            resultObje?.showDataCard = "${it?.cardType} - ${it?.countryCard} - ${it.valueItem}"
        }
//                resultObje?.cardProbability
        cardList.maxByOrNull { it?.valueItem ?: 0.0 }?.let {

            if ((it.valueItem) >= 0.70) {
                probability = it.valueItem
                selectedItem = it
                Log.i("test", "Selected  ${it}")
                Log.i("test", "Selected  ${selectedItem!!.cardType}")

                when {
                    selectedItem?.countryCard == CountryCard.Kenya.name -> {
                        when (selectedItem?.cardType) {

                            CardType.IDCardFront.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaIdCardFrontPath ?: ""
                            }

                            CardType.IDCardBack.name -> {
                                modelPath = MyPrefManager.getInstance(context!!)?.modelKenyaIdCardBackPath ?: ""
                            }
                        }
                    }
                }

                Log.i("test", "model path if  ${modelPath}")

            } else {
//                        Log.i("test", "model path values   ${it.valueItem}")
//                        Log.i("test", "model path not selected   ${modelPath}")
                selectedItem = null
            }


        }

        var processedTextNer = "{}"
        if (modelPath.isEmpty() == false) {
            processedTextNer = try {
                val result: PyObject? = pythonModPyt?.callAttr("process_text", lineTest.trim(), modelPath)
//                        Log.i("test", "processedText ner  ${result.toJava(String::class.java)}")
//                        Log.i("test", "result  = ${result.toJava(String::class.java)}")
                result?.toJava(String::class.java) ?: "{}"
            } catch (e: Exception) {
                Log.i("test", "error ", e)
                "{}"
            }
            Log.i("hello", "processedText ner ${processedTextNer}")
        } else {
            var hypoThesis = lineTest.trim().replace(" ", "")
            if (hypoThesis.contains("REPUBLICOFZIMBABWE", true) && hypoThesis.contains("NATIONALREGISTRATION", true)) {
                if (selectedItem == null)
                    selectedItem = CardTypeCountry()
                selectedItem?.countryCard = CountryCard.Zimbabwe.name
                selectedItem?.cardType = CardType.IDCardFront.name
                selectedItem?.valueItem = 100.0
                modelPath = MyPrefManager.getInstance(context!!)?.modelZimbabweIdCardPath ?: ""
            } else if (hypoThesis.contains("REPUBLIC OF SOUTHAFRICA", true) && hypoThesis.contains("Passport/Passeport", true)) {
                if (selectedItem == null)
                    selectedItem = CardTypeCountry()
                selectedItem?.countryCard = CountryCard.SouthAfrica.name
                selectedItem?.cardType = CardType.Passport.name
                selectedItem?.valueItem = 100.0
                modelPath = MyPrefManager.getInstance(context!!)?.modelSaPassportPath ?: ""
            }
//                    REPUBLIC OFZIMBABWE NATIONALREGISTRATION 24-092622G24CITF ID NUMBER SURNAME TADYANEMHANDU MARIA FIRST NAME 26/06/1976 DATE OF BRTH VILLAGE OFORIGIN CHINHOYI PLACE OF BIRTH 22/06/2009 DATE OF ISSU Signature of Holder
        }

        val mrzData = try {
            JSONObject(processedTextNer.trim())
        } catch (e: Exception) {
            Log.i("test", "MRZ data exception ${e} + ")
            JSONObject()
        }
        Log.i("hello", "Data ${mrzData}")
        if (selectedItem != null) {
            Log.i("hello", "Selected not null   ${selectedItem?.cardType}")

            Log.i("test", "Selected when elsee   ${selectedItem?.cardType}")
            when {

                selectedItem?.countryCard == CountryCard.Kenya.name -> {
                    when (selectedItem?.cardType) {
                        CardType.IDCardFront.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false
                            resultObje?.idNumber = mrzData.optString("ID NO","")
//                                    resultObje?.fullName = mrzData.getString("FULL NAME") //
                            resultObje?.dateOfBirth = mrzData.optString("DATE OF BIRTH" ,"")
                            resultObje?.sex = mrzData.optString("SEX" ,"")
                            resultObje?.placeOfBirth = mrzData.optString("PLACE OF BIRTH" ,"")
                            resultObje?.placeOfIssue = mrzData.optString("PLACE OF ISSUE" ,"")
                            resultObje?.dateOfIssue = mrzData.optString("DATE OF ISSUE" ,"")
                        }

                        CardType.IDCardBack.name -> {

                            resultObje?.cardType = selectedItem?.cardType ?: ""
                            resultObje?.country = selectedItem?.countryCard ?: ""
                            resultObje?.isPassport = false

                            resultObje?.district = mrzData.optString("DISTRICT" ,"")
                            resultObje?.division = mrzData.optString("DIVISION","")
                            resultObje?.location = mrzData.optString("LOCATION","")
                            resultObje?.subLocation = mrzData.optString("SUB-LOCATION" ,"")
                            resultObje?.identifier = mrzData.optString("IDENTIFIER","")
                            resultObje?.mrzNumber = mrzData.optString("MRZ","")
//                                    resultObje?.dateOfIssue = mrzData.getString("DATE OF ISSUE")
                        }
                    }
                }
            }
        } else {
            Log.i("hello", "Selected isss null   ${selectedItem?.cardType}")
        }
        resultObje?.cardProbability = probability.toString()
        picBitmap = originPicBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        return resultObje

    }

    fun detectDateFormat(dateStr: String): String? {
        val possibleFormats = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "MM-dd-yyyy",
            // Add more formats as needed
        )

        for (format in possibleFormats) {
            try {
                val formatter = SimpleDateFormat(format)
                formatter.isLenient = false
                formatter.parse(dateStr)
                return format
            } catch (e: ParseException) {
                // This format didn't match, try the next one
            }
        }

        // No formats matched
        return null
    }

    companion object {

        @JvmStatic
        var pythonModPyt: PyObject? = null

        private const val TIME_SLEEP_INTERVAL = 50
        var cpuThreadNum = 2
        var cpuPowerMode = ""
        var scoreThreshold = 0.4f
        var enableLiteFp16 = "true"

        private var results: MutableList<BaseResultModel> = ArrayList()

        var modelDir = ""
        var labelPath = ""
        var resultObje: BaseResult? = null
        var predictor: PPOCRv3? = null
        var result: OCRResult? = null
        private var picBitmap: Bitmap? = null
        private var originPicBitmap: Bitmap? = null
        private var texts: Array<String> = arrayOf()
        private var recScores: FloatArray = floatArrayOf()
        private var resultNum = 1.0f
        private var initialized = false
        private var isProcessingString = AtomicBoolean(false)
    }
}