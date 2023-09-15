package com.example.paddleocrlib
import android.graphics.*
import kotlin.math.atan2
import kotlin.math.roundToInt
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
//import androidx.renderscript.Allocation
//import androidx.renderscript.RenderScript
//import androidx.renderscript.ScriptIntrinsicBlur
//import androidx.renderscript.Element
//import androidx.renderscript.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min


class ImageUtils {
    companion object{

//        /*** is card detected ***/
//        fun isCardDetected(context: Context, bitmap: Bitmap, lowThreshold: Float =1.1f, highThreshold: Float =1.3f): Boolean {
//            val cannyBitmap: Bitmap = Canny.process(bitmap);// applyCannyEdgeDetection(context,bitmap ,lowThreshold ,highThreshold) // Your Canny edge image from applyCannyEdgeDetection
//            val minArea = 1000 // Adjust this value based on the size of the card in the image
//            val aspectRatioTolerance = 0.2f // Adjust this value to allow for variations in the card's aspect ratio
//            val cardDetected = isCardDetectedProcessing(cannyBitmap, minArea, aspectRatioTolerance)
//
//            if (cardDetected) {
//                println("Card detected!")
//                return true
//            } else {
//                println("Card not detected.")
//                return false
//            }
//        }
//
//        fun applyCannyEdgeDetection(context: Context, inputBitmap: Bitmap, lowThreshold: Float =1.1f, highThreshold: Float =1.3f): Bitmap {
////            val rs = RenderScript.create(context)
////
////            val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap)
////            val gradientAllocation = Allocation.createTyped(rs, inputAllocation.type)
////            val outputAllocation = Allocation.createTyped(rs, inputAllocation.type)
////
////            val cannyScript = ScriptC_canny(rs)
////
////            cannyScript.forEach_sobelGradient(inputAllocation, gradientAllocation)
////            cannyScript.forEach_cannyEdgeDetection(gradientAllocation, outputAllocation)
////
////            val outputBitmap = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, inputBitmap.config)
////            outputAllocation.copyTo(outputBitmap)
////
////            inputAllocation.destroy()
////            gradientAllocation.destroy()
////            outputAllocation.destroy()
////            cannyScript.destroy()
////            rs.destroy()
//
//            return inputBitmap
//        }
//
//        fun isCardDetectedProcessing(cannyBitmap: Bitmap, minArea: Int, aspectRatioTolerance: Float): Boolean {
//            val width = cannyBitmap.width
//            val height = cannyBitmap.height
//
//            val visited = Array(height) { BooleanArray(width) }
//            val queue: Queue<Pair<Int, Int>> = LinkedList()
//
//            fun isValid(x: Int, y: Int): Boolean {
//                return x >= 0 && x < width && y >= 0 && y < height && !visited[y][x] && cannyBitmap.getPixel(x, y) != 0
//            }
//
//            val directions = arrayOf(Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0))
//
//            for (y in 0 until height) {
//                for (x in 0 until width) {
//                    if (isValid(x, y)) {
//                        var minX = x
//                        var maxX = x
//                        var minY = y
//                        var maxY = y
//                        var area = 0
//
//                        queue.add(Pair(x, y))
//                        visited[y][x] = true
//
//                        while (queue.isNotEmpty()) {
//                            val current = queue.remove()
//                            area++
//
//                            for (direction in directions) {
//                                val newX = current.first + direction.first
//                                val newY = current.second + direction.second
//
//                                if (isValid(newX, newY)) {
//                                    minX = min(minX, newX)
//                                    maxX = max(maxX, newX)
//                                    minY = min(minY, newY)
//                                    maxY = max(maxY, newY)
//
//                                    queue.add(Pair(newX, newY))
//                                    visited[newY][newX] = true
//                                }
//                            }
//                        }
//
//                        val rectWidth = maxX - minX + 1
//                        val rectHeight = maxY - minY + 1
//                        val rectArea = rectWidth * rectHeight
//
//                        if (area >= minArea && rectArea > 0) {
//                            val aspectRatio = rectWidth.toFloat() / rectHeight.toFloat()
//                            if (aspectRatio >= (1 - aspectRatioTolerance) && aspectRatio <= (1 + aspectRatioTolerance)) {
//                                return true
//                            }
//                        }
//                    }
//                }
//            }
//
//            return false
//        }
//
//        /*** blury check ***/
//
//        fun isBluryImage(context: Context, image: Bitmap,threshold:Float = 1000f): Boolean {
//            val width = image.width
//            val height = image.height
//
//            val rs = RenderScript.create(context)
//            val inputAllocation = Allocation.createFromBitmap(rs, image)
//            val outputBitmap = Bitmap.createBitmap(width, height, image.config)
//            val outputAllocation = Allocation.createFromBitmap(rs, outputBitmap)
//
//            val blurDetectorScript = ScriptC_blur_detector(rs)
//            blurDetectorScript._inputImage = inputAllocation
//            blurDetectorScript.forEach_blur_detector(outputAllocation)
//
//            outputAllocation.copyTo(outputBitmap)
//            rs.destroy()
//
//            var varianceSum = 0.0
//            for (y in 0 until height) {
//                for (x in 0 until width) {
//                    val pixel = outputBitmap.getPixel(x, y)
//                    varianceSum += Color.red(pixel) * Color.red(pixel)
//                }
//            }
//            val variance = varianceSum / (width * height)
//
//            return variance < threshold
//        }
//
//
//        /*** enhance contrast ***/
//
//        fun enhanceLocalContrast(context: Context, input: Bitmap): Bitmap {
//            val width = input.width
//            val height = input.height
//
//            val output = Bitmap.createBitmap(width, height, input.config)
//
//            val rs = RenderScript.create(context)
//
//            // Convert input and output bitmaps to RenderScript allocations
//            val inputAllocation = Allocation.createFromBitmap(rs, input)
//            val outputAllocation = Allocation.createFromBitmap(rs, output)
//
//            // Create histogram allocation
//            val histogramType:Type = Type.Builder(rs, Element.U32(rs)).setX(256).create()
//            val histogramAllocation = Allocation.createTyped(rs, histogramType)
//
//            // Create and run histogram script
//            val histogramScript = ScriptIntrinsicHistogram.create(rs, Element.U8_4(rs))
//            histogramScript.setOutput(histogramAllocation)
//            histogramScript.forEach(inputAllocation)
//
//            // Create and run blur script
//            val blurRadius = 25f
//            val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
//            blurScript.setRadius(blurRadius)
//            blurScript.setInput(inputAllocation)
//            blurScript.forEach(outputAllocation)
//
//            // Create and run custom script to enhance local contrast
//            val enhanceScript = ScriptC_enhance(rs)
//            enhanceScript._width = width.toLong()
//            enhanceScript._height = height.toLong()
//            enhanceScript._input = inputAllocation
//            enhanceScript._blur = outputAllocation
//            enhanceScript._histogram = histogramAllocation
//            enhanceScript.forEach_root(outputAllocation)
//
//            // Copy the output allocation to the output bitmap
//            outputAllocation.copyTo(output)
//
//            // Clean up
//            inputAllocation.destroy()
//            outputAllocation.destroy()
//            histogramAllocation.destroy()
//            histogramScript.destroy()
//            blurScript.destroy()
//            enhanceScript.destroy()
//            rs.destroy()
//
//            return output
//        }
//
//
//        /*** remove noise ***/
//
//        fun gaussianBlur(context: Context, sourceBitmap: Bitmap, radius: Float): Bitmap {
//            // Create a RenderScript instance
//            val rs = RenderScript.create(context)
//
//            // Create input and output allocations
//            val inputAllocation = Allocation.createFromBitmap(rs, sourceBitmap)
//            val outputAllocation = Allocation.createTyped(rs, inputAllocation.type)
//
//            // Create and set up the ScriptIntrinsicBlur
//            val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
//            blurScript.setRadius(radius)
//            blurScript.setInput(inputAllocation)
//
//            // Run the blur script and copy the result to the output bitmap
//            val outputBitmap = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, sourceBitmap.config)
//            blurScript.forEach(outputAllocation)
//            outputAllocation.copyTo(outputBitmap)
//
//            // Clean up
//            inputAllocation.destroy()
//            outputAllocation.destroy()
//            blurScript.destroy()
//            rs.destroy()
//
//            return outputBitmap
//        }
//
//
//        /*** straight lines ***/
//        @JvmStatic
//        fun straightenLines(bitmap: Bitmap): Bitmap {
//            val grayBitmap = toGrayscale(bitmap)
//            val binaryBitmap = threshold(grayBitmap)
//            val lines = houghLines(binaryBitmap)
//            val averageAngle = averageAngle(lines)
//            return rotateBitmap(bitmap, averageAngle)
//        }
//
//        fun toGrayscale(bitmap: Bitmap): Bitmap {
//            val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(grayBitmap)
//            val paint = Paint()
//
//            val colorMatrix = ColorMatrix(
//                floatArrayOf(
//                    0.2989f, 0.5870f, 0.1140f, 0f, 0f,
//                    0.2989f, 0.5870f, 0.1140f, 0f, 0f,
//                    0.2989f, 0.5870f, 0.1140f, 0f, 0f,
//                    0f, 0f, 0f, 1f, 0f
//                )
//            )
//
//            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
//            canvas.drawBitmap(bitmap, 0f, 0f, paint)
//
//            return grayBitmap
//        }
//
//        fun threshold(bitmap: Bitmap, threshold: Int = 128): Bitmap {
//            val binaryBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
//            for (y in 0 until bitmap.height) {
//                for (x in 0 until bitmap.width) {
//                    val gray = Color.red(bitmap.getPixel(x, y))
//                    val binaryColor = if (gray < threshold) Color.BLACK else Color.WHITE
//                    binaryBitmap.setPixel(x, y, binaryColor)
//                }
//            }
//            return binaryBitmap
//        }
//
//        data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int)
//
//        fun houghLines(bitmap: Bitmap, threshold: Int = 100): List<Line> {
//            val width = bitmap.width
//            val height = bitmap.height
//            val diagonal = kotlin.math.sqrt((width * width + height * height).toDouble()).toInt()
//            val maxTheta = 180
//
//            // Initialize the accumulator array
//            val accumulator = Array(diagonal * 2) { IntArray(maxTheta) }
//
//            // Fill the accumulator array
//            for (y in 0 until height) {
//                for (x in 0 until width) {
//                    if (Color.red(bitmap.getPixel(x, y)) == 0) { // Black pixel
//                        for (theta in 0 until maxTheta) {
//                            val radians = Math.toRadians(theta.toDouble())
//                            val rho = x * kotlin.math.cos(radians) + y * kotlin.math.sin(radians)
//                            val rhoIndex = diagonal + rho.roundToInt()
//                            accumulator[rhoIndex][theta]++
//                        }
//                    }
//                }
//            }
//
//            // Extract the lines from the accumulator array
//            val lines = mutableListOf<Line>()
//            for (rhoIndex in 0 until accumulator.size) {
//                for (theta in 0 until maxTheta) {
//                    if (accumulator[rhoIndex][theta] >= threshold) {
//                        val rho = rhoIndex - diagonal
//                        val radians = Math.toRadians(theta.toDouble())
//                        val cos = kotlin.math.cos(radians)
//                        val sin = kotlin.math.sin(radians)
//
//                        val x1 = (rho - height * sin) / cos
//                        val y1 = 0.0
//                        val x2 = (rho - width * sin) / cos
//                        val y2 = width.toDouble()
//
//                        lines.add(Line(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt()))
//                    }
//                }
//            }
//
//            return lines
//        }
//
//
//        fun averageAngle(lines: List<Line>): Float {
//            var sumAngles = 0.0
//            for (line in lines) {
//                val angle = atan2((line.y2 - line.y1).toDouble(), (line.x2 - line.x1).toDouble())
//                sumAngles += angle
//            }
//            return (sumAngles / lines.size * (180.0 / Math.PI)).toFloat()
//        }
//        fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
//            val matrix = Matrix()
//            matrix.postRotate(degrees)
//            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//        }
//
//
//


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

    }
}