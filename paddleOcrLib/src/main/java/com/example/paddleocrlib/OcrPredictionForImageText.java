package com.example.paddleocrlib;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


import com.baidu.paddle.fastdeploy.RuntimeOption;
import com.baidu.paddle.fastdeploy.pipeline.PPOCRv3;
import com.baidu.paddle.fastdeploy.vision.OCRResult;
import com.baidu.paddle.fastdeploy.vision.ocr.Classifier;
import com.baidu.paddle.fastdeploy.vision.ocr.DBDetector;
import com.baidu.paddle.fastdeploy.vision.ocr.Recognizer;
import com.kurzdigital.mrz.MrzInfo;
import com.kurzdigital.mrz.MrzParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class OcrPredictionForImageText {
    static public Bitmap picBitmap;
    private Bitmap originPicBitmap;

    public String modelDir = "";

    BaseResult resultObje ;
    public String labelPath = "";
    static public int cpuThreadNum = 2;
    static public String cpuPowerMode = "";
    static public float scoreThreshold = 0.4f;
    static public String enableLiteFp16 = "true";

    private String[] texts;
    private float[] recScores;
    PPOCRv3 predictor;

    OCRResult result;
    private float resultNum = 1.0f;


    private boolean initialized;
    private List<BaseResultModel> results = new ArrayList<>();


    private static final int TIME_SLEEP_INTERVAL = 50;
   public OcrPredictionForImageText(Bitmap uri , Context context , float confidencce)
    {
        Log.d("Test" , "22222222222222");
        //context.startActivity(new Intent(context, OcrActivity.class));
        Log.d("Test" , "555555");
        predictor = new PPOCRv3();
        result = new OCRResult();
        resultObje = new BaseResult();

          //  String path = Utils.getRealPathFromURI(context, uri);

      //    picBitmap = Utils.decodeBitmap(path, 1920, 1080);

       // context.startActivity(new Intent(context , EmptyActivity.class).putExtra("path" , path));
        if (checkAndUpdateSettings(context))
        {
            resultNum = confidencce;

            picBitmap = uri;

            //picBitmap = Utils.decodeBitmap(path, 1920, 1080);
            originPicBitmap = picBitmap.copy(Bitmap.Config.ARGB_8888, true);

            result = predictor.predict(picBitmap, true);


        }
        else
        {
            Toast.makeText(context, " Predictor is not initialized ", Toast.LENGTH_SHORT).show();

        }



    }
    public boolean checkAndUpdateSettings(Context context) {

        modelDir = context.getString(R.string.OCR_MODEL_DIR_DEFAULT);
        labelPath =  context.getString(R.string.OCR_REC_LABEL_DEFAULT);
        cpuThreadNum = Integer.parseInt(context.getString(R.string.CPU_THREAD_NUM_DEFAULT));
        cpuPowerMode = context.getString(R.string.CPU_POWER_MODE_DEFAULT);
        enableLiteFp16 = "true";
        scoreThreshold = Float.parseFloat(context.getString(R.string.SCORE_THRESHOLD_DEFAULT));

        String realModelDir = context.getCacheDir() + "/" + modelDir;;
        String detModelName = "ch_PP-OCRv3_det_infer";
        String clsModelName = "ch_ppocr_mobile_v2.0_cls_infer";
        String recModelName = "ch_PP-OCRv3_rec_infer";
        String realDetModelDir = realModelDir + "/" + detModelName;
        String realClsModelDir = realModelDir + "/" + clsModelName;
        String realRecModelDir = realModelDir + "/" + recModelName;
        String srcDetModelDir = modelDir + "/" + detModelName;
        String srcClsModelDir = modelDir + "/" + clsModelName;
        String srcRecModelDir = modelDir + "/" + recModelName;
        Utils.copyDirectoryFromAssets(context, srcDetModelDir, realDetModelDir);
        Utils.copyDirectoryFromAssets(context, srcClsModelDir, realClsModelDir);
        Utils.copyDirectoryFromAssets(context, srcRecModelDir, realRecModelDir);
        String realLabelPath = context.getCacheDir() + "/" +  labelPath;
        Utils.copyFileFromAssets(context, labelPath, realLabelPath);

        String detModelFile = realDetModelDir + "/" + "inference.pdmodel";
        String detParamsFile = realDetModelDir + "/" + "inference.pdiparams";
        String clsModelFile = realClsModelDir + "/" + "inference.pdmodel";
        String clsParamsFile = realClsModelDir + "/" + "inference.pdiparams";
        String recModelFile = realRecModelDir + "/" + "inference.pdmodel";
        String recParamsFile = realRecModelDir + "/" + "inference.pdiparams";
        String recLabelFilePath = realLabelPath; // ppocr_keys_v1.txt
        RuntimeOption detOption = new RuntimeOption();
        RuntimeOption clsOption = new RuntimeOption();
        RuntimeOption recOption = new RuntimeOption();
        detOption.setCpuThreadNum( cpuThreadNum);
        clsOption.setCpuThreadNum( cpuThreadNum);
        recOption.setCpuThreadNum( cpuThreadNum);
        detOption.setLitePowerMode( cpuPowerMode);
        clsOption.setLitePowerMode( cpuPowerMode);
        recOption.setLitePowerMode( cpuPowerMode);
        if (Boolean.parseBoolean( enableLiteFp16)) {
            detOption.enableLiteFp16();
            clsOption.enableLiteFp16();
            recOption.enableLiteFp16();
        }
        DBDetector detModel = new DBDetector(detModelFile, detParamsFile, detOption);
        Classifier clsModel = new Classifier(clsModelFile, clsParamsFile, clsOption);
        Recognizer recModel = new Recognizer(recModelFile, recParamsFile, recLabelFilePath, recOption);
        predictor.init(detModel, clsModel, recModel);
        return predictor.initialized();


    }
   public BaseResult detail(Context context) {
        if (result.initialized())
        {
            SystemClock.sleep(TIME_SLEEP_INTERVAL * 10);
            if (result.initialized())
            {
                texts = result.mText;
                recScores = result.mRecScores;
                int[][] box = result.mBoxes;
                String line = "";
                initialized = result.initialized();
                if (initialized) {
                    for (int i = 0; i < texts.length; i++) {
                        if (recScores[i] > resultNum) {
                            line = line + texts[i] + "\n ";
//                            Log.d("BoxValues" , Arrays.toString(box[i]) );
                            Log.i("test" , "line "+texts[i] + " => boxes => "+ Arrays.toString(box[i]));


                            results.add(new BaseResultModel(i + 1, texts[i], recScores[i] ,new int[0]));
                        }
                    }
                }

                Log.d("GetText", "line we get from result \n \n"+line.toString());
                boolean passport =
                        line.toLowerCase().contains("passport")
                                || line.toUpperCase().contains("assport")
                                || line.toUpperCase().contains("passspor")
                                || line.toUpperCase().contains("asspor")
                                || line.toUpperCase().contains("<<<<<<<<<<")
                                || line.toUpperCase().contains("passeport")
                                || line.toUpperCase().contains("asseport")
                                || line.toUpperCase().contains("assepor");


                String surnameRegex = "Surname:\\s*(\\S+)";
                String namesRegex = "Names:\\s*(\\S+(?:\\s+\\S+)*)";
                String sexRegex = "Sex\\s*(\\S+)";
                String nationalityRegex = "Nationality:\\s*(\\S+)";
                String idNumberRegex = "Identity Number\\s*(\\S+)";
                String dobRegex = "Date of Birth:\\s*(\\S+\\s+\\S+\\s+\\S+)";
                String birthCountryRegex = "Country of Birth\\s*(\\S+)";
                String statusRegex = "Status:\\s*(\\S+)";

                if (passport ) {
                    Log.d("GetText" ,"   passwpord  ");
                    System.out.println("secound last  line value " + results.get(results.size() - 2).getName());
                    System.out.println("secound last line value " + results.get(results.size() - 2).getName().length());
                    System.out.println(" last line value " + results.get(results.size() - 1).getName());



                    MrzInfo mrzInfo = MrzParser.parse(results.get(results.size() - 2).getName() + results.get(results.size() - 1).getName());
                    System.out.println(" dateOfExpiry value " + mrzInfo.dateOfExpiry);
                    System.out.println(" sex value " + mrzInfo.sex);
                    System.out.println(" dateOfBirth value " + mrzInfo.dateOfBirth);
                    System.out.println(" secondaryIdentifier value " + mrzInfo.secondaryIdentifier);
                    System.out.println("primaryIdentifier  value " + mrzInfo.primaryIdentifier);
                    System.out.println("nationality value " + mrzInfo.nationality);
                    System.out.println("issuingState  value " + mrzInfo.issuingState);
                    System.out.println(" documentNumber value " + mrzInfo.documentNumber);
                    resultObje.setIdNumber(mrzInfo.documentNumber);
                    resultObje.setSurname(mrzInfo.primaryIdentifier);
                    resultObje.setDateOfBirth(mrzInfo.dateOfBirth);
                    resultObje.setSex(mrzInfo.sex);
                    resultObje.setNationality(mrzInfo.nationality);



                    resultObje.setPassport(true);

                } else {
                    if (line.toUpperCase().contains("AFRICA")
                            || line.toUpperCase().contains("SOUTHAFRICA")
                            || line.toUpperCase().contains("OUTHAFRICA")

                    ) {
                        Log.d("GetText" ,"  SouthAgirca cardd ");
                        Matcher surnameMatcher = Pattern.compile(surnameRegex).matcher(line);
                        Matcher namesMatcher = Pattern.compile(namesRegex).matcher(line);
                        Matcher sexMatcher = Pattern.compile(sexRegex).matcher(line);
                        Matcher nationalityMatcher = Pattern.compile(nationalityRegex).matcher(line);
                        Matcher idNumberMatcher = Pattern.compile(idNumberRegex).matcher(line);
                        Matcher dobMatcher = Pattern.compile(dobRegex).matcher(line);
                        Matcher birthCountryMatcher = Pattern.compile(birthCountryRegex).matcher(line);
                        Matcher statusMatcher = Pattern.compile(statusRegex).matcher(line);
                        if (surnameMatcher.find()) {
                            resultObje.setSurname(surnameMatcher.group(1));
                        }
                        if (namesMatcher.find()) {
                            resultObje.setLastName(namesMatcher.group(1));
                        }
                        if (sexMatcher.find()) {
                            resultObje.setSex(sexMatcher.group(1));
                        }
                        if (nationalityMatcher.find()) {
                            resultObje.setNationality(nationalityMatcher.group(1));

                        }
                        if (idNumberMatcher.find()) {
                            resultObje.setIdNumber(idNumberMatcher.group(1));
                        }
                        if (dobMatcher.find()) {
                            resultObje.setDateOfBirth(dobMatcher.group(1));
                        }
                        if (birthCountryMatcher.find()) {
                            resultObje.setBirthCountry(birthCountryMatcher.group(1));
                        }
                        if (statusMatcher.find()) {
                            resultObje.setStatus(statusMatcher.group(1));
                        }
                        resultObje.setAfricaCard(true);


                    } else {

                        if (line.toUpperCase().contains("ZIMBABWE")
                                || line.toUpperCase().contains("IMBABWE")
                                || line.toUpperCase().contains("IMBABW")
                                || line.toUpperCase().contains("ZIMBABW")
                                || line.toUpperCase().contains("ZINEABIVE")
                        ) {
                            Log.d("GetText" ,"  ZIMBABWE cardd ");
                                    resultObje.setZimbabwe(true);
                            String idNumberPattern = "\\bID\\sNUMBER\\s+(\\d{2}-\\d{6}\\s\\w{3})";
                            String surnamePattern = "\\bSURNAME\\s+(\\w+)";
                            String firstNamePattern = "\\bFIRST\\sNAME\\s+(\\w+\\s?\\w*)";
                            String dateOfBirthPattern = "\\bDATE\\sof\\sBIRTH\\s+(\\d{2}/\\d{2}/\\d{4})";
                            String villageOfOriginPattern = "\\bVILLAGE\\sof\\sORIGIN\\s+(\\w+)";
                            String placeOfBirthPattern = "\\bPLACE\\sof\\sBIRTH\\s+(.+)";
                            String dateOfIssuePattern = "\\bDATE\\sof\\sISSUE\\s+(\\d{2}/\\d{2}/\\d{4})";

                            Pattern idNumberRegex2 = Pattern.compile(idNumberPattern);
                            Pattern surnameRegex2 = Pattern.compile(surnamePattern);
                            Pattern firstNameRegex = Pattern.compile(firstNamePattern);
                            Pattern dateOfBirthRegex = Pattern.compile(dateOfBirthPattern);
                            Pattern villageOfOriginRegex = Pattern.compile(villageOfOriginPattern);
                            Pattern placeOfBirthRegex = Pattern.compile(placeOfBirthPattern);
                            Pattern dateOfIssueRegex = Pattern.compile(dateOfIssuePattern);
                            // Matcher objects
                            Matcher idNumberMatcher = idNumberRegex2.matcher(line);
                            Matcher surnameMatcher = surnameRegex2.matcher(line);
                            Matcher firstNameMatcher = firstNameRegex.matcher(line);
                            Matcher dateOfBirthMatcher = dateOfBirthRegex.matcher(line);
                            Matcher villageOfOriginMatcher = villageOfOriginRegex.matcher(line);
                            Matcher placeOfBirthMatcher = placeOfBirthRegex.matcher(line);
                            Matcher dateOfIssueMatcher = dateOfIssueRegex.matcher(line);

                            resultObje.setIdNumber(idNumberMatcher.find() ? idNumberMatcher.group(1) : "Please enter value");
                            resultObje.setSurname(surnameMatcher.find() ? surnameMatcher.group(1) : "palce holder ");
                            resultObje.setLastName(firstNameMatcher.find() ? firstNameMatcher.group(1) : "");
                            resultObje.setDateOfBirth(dateOfBirthMatcher.find() ? dateOfBirthMatcher.group(1) : "");
                            resultObje.setVillageOfOrigin(villageOfOriginMatcher.find() ? villageOfOriginMatcher.group(1) : "");
                            resultObje.setPlaceOfBirth(placeOfBirthMatcher.find() ? placeOfBirthMatcher.group(1) : "");
                            resultObje.setDateOfIssue(dateOfIssueMatcher.find() ? dateOfIssueMatcher.group(1) : "");

                            Log.d("GetText" , "value in object "+resultObje.getSurname()+" 22 "+resultObje.getIdNumber());

                        } else {

                          //  Toast.makeText(context, " Card unknown", Toast.LENGTH_SHORT).show();

                        }


                    }
                }

                picBitmap = originPicBitmap.copy(Bitmap.Config.ARGB_8888, true);


                return resultObje;
            }
            else
            {
                Toast.makeText(context, "Predictor not initialized", Toast.LENGTH_SHORT).show();

                return null;
            }

        }
        else
        {
            Log.d("GetText" , "result is null");
            //Toast.makeText(context, " Result is null ", Toast.LENGTH_SHORT).show();
            return null;

        }



    }

}
