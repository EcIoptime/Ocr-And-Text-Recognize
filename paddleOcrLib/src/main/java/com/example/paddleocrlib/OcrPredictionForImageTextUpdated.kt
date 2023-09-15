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

class OcrPredictionForImageTextUpdated( var context: Context, var confidencce: Float, var errorHandler: Consumer<BaseResult?>) {

    init {
        Log.i("test", "init function ")

        if (predictor == null)
            predictor = PPOCRv3()

        if (pythonModPyt == null) {
            val py = Python.getInstance()
            pythonModPyt = py.getModule("SpacyModule")
        }
//        var  result = OCRResult()
//        Log.d("test", "result obj initialized  = ${result?.initialized()}")
        if (checkAndUpdateSettings(context) == true) {
            Log.i("test", "checkAndUpdateSettings is trueee")
            resultNum = confidencce
        } else {
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, " Predictor is not initialized ", Toast.LENGTH_SHORT).show() }
        }
    }

    fun processImage(bitmap: Bitmap): BaseResult? {
        var baseResult = BaseResult()
        if (predictor?.initialized() == true ) {
            resultNum = confidencce
            var picBitmap = bitmap //Utils.decodeBitmap(path, 1920, 1080);
            var originPicBitmap = picBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            var result = predictor?.predict(picBitmap, true)
            val obj = detailNew(context, baseResult, result)
            obj?.cardImage = bitmap
            errorHandler.accept(obj)
        } else {
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, " Predictor is not initialized ", Toast.LENGTH_SHORT).show() }
        }
        return baseResult
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


    fun detailNew(context: Context?, resultObje: BaseResult, result: OCRResult?): BaseResult? {
        var results: MutableList<BaseResultModel> = ArrayList()
        val texts = result?.mText ?: arrayOf()
        val recScores = result?.mRecScores ?: floatArrayOf()
        val boxesWords = result?.mBoxes

        var line = ""
        var lineTest = ""
        val initialized = result?.initialized() ?: false
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

        resultObje.rawLine = lineTest

        var modelPathCard = MyPrefManager.getInstance(context!!)?.modelAllCardPath ?: ""
        val processedText = try {
            Log.i("hello", "line text at start   ${lineTest}")
            var result: PyObject? = pythonModPyt?.callAttr("detect_text_classify", lineTest, modelPathCard)
//                    Log.i("test", "processedText  ${result.toJava(String::class.java)}")
            result?.toJava(String::class.java) ?: "{}"
        } catch (e: Exception) {
            Log.i("hello", "error ", e)
            ""
        }
        Log.i("hello", "processedText at start   ${processedText}")

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
//                Log.i("test", "Selected  ${it}")
//                Log.i("test", "Selected  ${selectedItem!!.cardType}")

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
            } else {
                selectedItem = null
            }
        }

        var processedTextNer = "{}"
        if (modelPath.isEmpty() == false) {
            processedTextNer = try {
                val result: PyObject? = pythonModPyt?.callAttr("process_text", lineTest.trim(), modelPath)
                result?.toJava(String::class.java) ?: "{}"
            } catch (e: Exception) {
                Log.i("test", "error ", e)
                "{}"
            }
            Log.i("hello", "classification ner ${processedTextNer}")
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
            Log.i("hello", "Ner parsing exception ${e} + ")
            JSONObject()
        }
        Log.i("hello", "Data ${mrzData}")
        if (selectedItem != null) {
            Log.i("hello", "Selected  ${selectedItem?.cardType}")
            when {

                selectedItem?.countryCard == CountryCard.Kenya.name -> {
                    when (selectedItem?.cardType) {
                        CardType.IDCardFront.name -> {

                            resultObje.cardType = selectedItem?.cardType ?: ""
                            resultObje.country = selectedItem?.countryCard ?: ""
                            resultObje.isPassport = false
                            resultObje.idNumber = mrzData.optString("ID NO","")
                            resultObje.name = mrzData.optString("NAME","") //
                            resultObje.dateOfBirth = mrzData.optString("DATE OF BIRTH" ,"")
                            resultObje.sex = mrzData.optString("SEX" ,"")
                            resultObje.placeOfBirth = mrzData.optString("PLACE OF BIRTH" ,"")
                            resultObje.placeOfIssue = mrzData.optString("PLACE OF ISSUE" ,"")
                            resultObje.dateOfIssue = mrzData.optString("DATE OF ISSUE" ,"")
                        }

                        CardType.IDCardBack.name -> {

                            resultObje.cardType = selectedItem?.cardType ?: ""
                            resultObje.country = selectedItem?.countryCard ?: ""
                            resultObje.isPassport = false

                            resultObje.district = mrzData.optString("DISTRICT","")
                            resultObje.division = mrzData.optString("DIVISION" ,"")
                            resultObje.location = mrzData.optString("LOCATION" ,"")
                            resultObje.subLocation = mrzData.optString("SUB-LOCATION" ,"")
                            resultObje.identifier = mrzData.optString("IDENTIFIER" ,"")
                            resultObje.mrzNumber = mrzData.optString("MRZ" ,"")
//                            Log.i("hello" ,"data holding ${resultObje.mrzNumber}")
//                                    resultObje?.dateOfIssue = mrzData.getString("DATE OF ISSUE")
                        }
                    }
                }
            }
        } else {
//            Log.i("hello", "Selected isss null   ${selectedItem?.cardType}")
        }
        resultObje.cardProbability = probability.toString()
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

    fun isInitialized(): Boolean {
        return predictor?.initialized() ?: false
    }

    companion object {

        @JvmStatic
        var pythonModPyt: PyObject? = null

        private const val TIME_SLEEP_INTERVAL = 50
        var cpuThreadNum = 2
        var cpuPowerMode = ""
        var scoreThreshold = 0.4f
        var enableLiteFp16 = "true"
        var modelDir = ""
        var labelPath = ""
        var predictor: PPOCRv3? = null

        private var resultNum = 1.0f
    }
}