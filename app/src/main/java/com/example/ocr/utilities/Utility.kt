package com.example.ocr.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.ocr.R
import com.google.android.material.snackbar.Snackbar

import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class Utility {
    companion object {
        var pg: KProgressHUD? = null

        @JvmStatic
        fun showProgressD(context: Context, msg: String = "Please wait") {
            try {
                hideProgressD(context)
                pg = KProgressHUD.create(context)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel(msg, context.resources.getColor(R.color.white))
                    .setBackgroundColor(context.resources.getColor(R.color.redNewTheme))
                    //.setDetailsLabel("Downloading data")
                    .setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f)
                pg?.show()
            } catch (e: Exception) {
                Log.i("test", "e  ${e}")
            } catch (e: Error) {
                Log.i("test", "e  ${e}")
            }
        }

        @JvmStatic
        fun hideProgressD(context: Context) {
            try {
                if (pg != null && pg?.isShowing == true) pg?.dismiss()
            } catch (e: Exception) {
                Log.i("test", "e  ${e}")
            }
        }

        @JvmStatic
        fun shareText(context: Context) {
            val shareBody = "Here is the share content body"
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
            context.startActivity(Intent.createChooser(sharingIntent, "Share our app to other users"))
        }

        @JvmStatic
        fun encryptThisString(input: String): String {
            try {
                // getInstance() method is called with algorithm SHA-512
                val md = MessageDigest.getInstance("SHA-512")

                // digest() method is called
                // to calculate message digest of the input string
                // returned as array of byte
                val messageDigest = md.digest(input.toByteArray())

                // Convert byte array into signum representation
                val no = BigInteger(1, messageDigest)

                // Convert message digest into hex value
                /*var hashtext = no.toString(16)

                Log.i("test" ,"hashtext.length ${hashtext.length}")
                // Add preceding 0s to make it 32 bit
                while (hashtext.length < 32) {
                    hashtext = "0$hashtext"
                }*/
                var hashtext = String.format("%0" + (messageDigest.size shl 1) + "X", no)

                // return the HashText
                return hashtext
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            // For specifying wrong message digest algorithms
        }

//        @JvmStatic
//        fun showSnackBar(msg:String,parentLayout: View,context:Context){
//            //val parentLayout = findViewById<View>(android.R.id.content)
//            Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
//                .setAction("CLOSE") { }
//                .setActionTextColor(context.resources.getColor(android.R.color.holo_red_light))
//                .show()
//        }

        @JvmStatic
        fun showSnackBarWithAction(msg: String, parentLayout: View, context: Context, action: String, callBack: () -> Unit) {
            //val parentLayout = findViewById<View>(android.R.id.content)
            Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
                .setAction(action) { callBack.invoke() }
                .setActionTextColor(context.resources.getColor(android.R.color.holo_red_light))
                .show()
        }

        @JvmStatic
        fun isValidEmail(target: CharSequence): Boolean {
            return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }

        @JvmStatic
        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = activity.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        @JvmStatic
        fun handleNetworkErrors(e: Exception, context_: Activity) {
            Log.i("test", " e $e")
            Handler(Looper.getMainLooper()).post {
                when (e) {
                    is UnknownHostException, is NoRouteToHostException -> context_.showSnackBar("Please check internet connections")
                    is SocketTimeoutException -> context_.showSnackBar("Connection time out, please try again")
                    is ConnectException -> context_.showSnackBar("Unable to connect PLease check internet")
                    else -> context_.showSnackBar("Error: $e")
                }
            }
        }

        @JvmStatic
        fun maxSizeImage(imagePath: String?): Boolean {
            var temp = false
            val file = File(imagePath)
            val length: Long = file.length()
            Log.i("test", "image size ${length}  file exists ${file.exists()}")
            if (length < (5 * 1048576)) // 5 mb
                temp = true
            return temp
        }

        @JvmStatic
        fun getImagePath(contentURI: Uri, context: Context, fileName: String): String? {
            var f: File? = null
            return try {
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = context.contentResolver?.query(contentURI, filePathColumn, null, null, null)
                cursor?.moveToFirst()
                val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
                val picturePath = columnIndex?.let { cursor.getString(it) }
                f = File(picturePath)
//                Log.i("test", "file path  ${f.exists()}  ${f.absolutePath}")

                var dir = File(context.applicationContext.cacheDir, "temp")
                dir.mkdir()
                var file = File(dir, fileName + "." + f.absolutePath.split(".").lastOrNull())
                if (!file.exists()) file.createNewFile()


                f.copyTo(file, true)?.absolutePath
//                file.absolutePath

            } catch (e: IOException) {
                Log.i("test", "eeeeeeeeeee  ${e}")
                ""
            }
        }

        @JvmStatic
        fun trackCalls(number: Int) {
            Log.i("test", "Tracking :: (start)")
            Exception().stackTrace.forEachIndexed { index, it ->
                if (index < number) {
                    if (it.isNativeMethod) return@forEachIndexed
                    if (it.fileName == "Looper.java") return@forEachIndexed
                    if (it.fileName == "ActivityThread.java") return@forEachIndexed
                    if (it.fileName == "Handler.java") return@forEachIndexed
                    if (it.fileName == "ZygoteInit.java") return@forEachIndexed
                    if (it.fileName == "AsyncTask.java") return@forEachIndexed
                    if (it.fileName == "CancellableAsyncTask.java") return@forEachIndexed
                    if (it.methodName == "trackCalls") return@forEachIndexed
                    if (it.methodName == "Activity.java") return@forEachIndexed
                    Log.i("test", "onBatchScanResults Tracking :: ${it.fileName} ${it.methodName} : ${it.lineNumber}")
                }
            }
            Log.i("test", "Tracking :: (end)")
        }

        @JvmStatic
        fun estimatedDistanceV2(measuredPower: Double, rssiStrengthIndicator: Double): Double {
            val x = 0.42093
            val y = 6.9476
            val z = 0.549

            val ratio = (rssiStrengthIndicator * 1.0) / measuredPower

            if (ratio < 1.0) {
                return trimmedDistance(Math.pow(10.0, ratio))
            } else {
                return trimmedDistance((x) * Math.pow(ratio, y) + z)
            }
        }

        @JvmStatic
        private fun trimmedDistance(ed: Double): Double {
            val bd = BigDecimal(ed).setScale(4, RoundingMode.HALF_UP)
            return bd.toDouble()
        }

        fun getProximity(paramDouble: Double): ProximityType {
            if (paramDouble <= 0.5) {
                return ProximityType.IMMEDIATE
            }
            return if (paramDouble > 0.5 && paramDouble <= 3.0) {
                ProximityType.NEAR
            } else ProximityType.FAR
        }

        enum class ProximityType {
            IMMEDIATE, NEAR, FAR
        }

        fun isNetworkConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            return cm!!.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        }

        @JvmStatic
        fun showSnackBar(msg: String, parentLayout: View, context: Context) {
            //val parentLayout = findViewById<View>(android.R.id.content)
            Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
                .setAction("CLOSE") { }
                .setActionTextColor(context.resources.getColor(android.R.color.holo_red_light))
                .show()
        }


        @JvmStatic
        fun getBitmapFromAssett(context: Context, filePath: String): Bitmap? {

            var bitmap: Bitmap? = null
            try {

                val assetManager = context.assets
                val inputStream = assetManager.open(filePath)
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
//            bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                Log.i("test", "error", e)
                e.printStackTrace()
            }
            return bitmap
        }

        @JvmStatic
        fun getAssetFile(context: Context, assetFileName: String): File {
            val assetManager = context.assets
            val inputStream = assetManager.open(assetFileName)
            val outputFile = File.createTempFile(assetFileName, null, context.cacheDir)
            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)
            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            outputStream.close()
            inputStream.close()
            return outputFile
        }

        @JvmStatic
        fun getAssetFileTemp(context: Context, assetFileName: String): File {
            val assetManager = context.assets
            val inputStream = assetManager.open("fng/" + assetFileName)
            var oldFile = File(context.cacheDir, assetFileName)
            Log.i("test" ,"old file exists ${oldFile.exists()}")
            var outputFile: File? = null
            outputFile = if (!oldFile.exists())
                File.createTempFile(assetFileName, null, context.cacheDir)
            else
                oldFile
            outputFile = File(context.cacheDir ,assetFileName )
//            return outputFile
            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)
            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            outputStream.close()
            inputStream.close()
            return outputFile!!
        }


        @JvmStatic
        fun hasFingerprint(bitmap: Bitmap?): Boolean {
            if (bitmap == null) return false
            val pixelArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixelArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val threshold = 20
            val width = bitmap.width
            val height = bitmap.height
            var fingerFound = false
            var whitePixelCount = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixelArray[y * width + x]
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    if (r <= threshold && g <= threshold && b <= threshold) {
                        fingerFound = true
                        break
                    } else if (r > 230 && g > 230 && b > 230) {
                        whitePixelCount++
                    }
                }
                if (fingerFound) break
            }

            return fingerFound && whitePixelCount < width * height / 2
        }


        /**** zip and email folder ***/

        fun zipFolder(sourceFolderPath: String, outputFolderPath: String, zipFileName: String): File? {
            val sourceFile = File(sourceFolderPath)
            val outputFile = File(outputFolderPath, zipFileName)

            try {
                val fos = FileOutputStream(outputFile)
                val zos = ZipOutputStream(BufferedOutputStream(fos))

                zipFile(sourceFile, sourceFile.name, zos)

                zos.close()
                fos.close()

                return outputFile

            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        private fun zipFile(sourceFile: File, fileName: String, zos: ZipOutputStream) {
            if (sourceFile.isHidden) return

            if (sourceFile.isDirectory) {
                val fileList = sourceFile.list()
                if (fileList != null && fileList.isNotEmpty()) {
                    for (file in fileList) {
                        zipFile(File(sourceFile, file), "$fileName/$file", zos)
                    }
                } else {
                    val entry = ZipEntry("$fileName/")
                    zos.putNextEntry(entry)
                    zos.closeEntry()
                }
            } else {
                val fis = FileInputStream(sourceFile)
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)

                val buffer = ByteArray(1024)
                var length: Int

                while (fis.read(buffer).also { length = it } > 0) {
                    zos.write(buffer, 0, length)
                }

                zos.closeEntry()
                fis.close()
            }
        }

        @JvmStatic
        fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }

        @JvmStatic
        fun copyZipFromAssetsToCacheAndUnzip(context: Context, zipFileName: String): String? {
            try {
                val assetManager = context.assets
                val cacheDir = context.cacheDir
                val zipFilePath = File(cacheDir, zipFileName)
                zipFilePath.delete()

                // Copy the zip file from assets to cache directory
                val inputStream = assetManager.open(zipFileName)
                val outputStream = FileOutputStream(zipFilePath)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                // Unzip the file
                val unzipDir = File(cacheDir, "ner_models")
                unzipDir.mkdirs()

                val zipInputStream = ZipInputStream(zipFilePath.inputStream())
                var entry = zipInputStream.nextEntry

                while (entry != null) {
                    val entryFile = File(unzipDir, entry.name)

                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        val entryOutputStream = FileOutputStream(entryFile)
                        zipInputStream.copyTo(entryOutputStream)
                        entryOutputStream.close()
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }

                zipInputStream.close()

                return File(unzipDir?.absolutePath ?: "", zipFileName.dropLast(4)).absolutePath
            } catch (e: Exception) {
                Log.i("test", "exception ", e)
                e.printStackTrace()
            }
            return ""
        }



        @JvmStatic
        fun isTablet(context: Context): Boolean {
            // Get the device's configuration
            val configuration: Configuration = context.resources.configuration

            // Check if the device is in tablet mode based on the screen size and density
            val screenSize: Int = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            val densityDpi = context.resources.displayMetrics.densityDpi
            return ((screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                    screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE ||
                    screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL)
                    && densityDpi == DisplayMetrics.DENSITY_XHIGH)
        }

    }

}
