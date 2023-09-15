package com.example.paddleocrlib.utils

import android.graphics.*
import android.util.Log

class ImageUtilsOld {
    companion object{

        /*** upscale image ***/
        fun upscaleImageWithFixedWidth(bitmap: Bitmap, fixedWidth: Int): Bitmap {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            val aspectRatio = originalHeight.toFloat() / originalWidth.toFloat()

            val newWidth = fixedWidth
            val newHeight = (newWidth * aspectRatio).toInt()

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        /*** check if have glare ***/
        fun hasGlare(bitmap: Bitmap, threshold:Double = 99.0  ): Boolean {
            // Convert bitmap to grayscale
            val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    val color = bitmap.getPixel(x, y)
                    val gray = (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114).toInt()
                    grayscaleBitmap.setPixel(x, y, Color.argb(Color.alpha(color), gray, gray, gray))
                }
            }

            // Calculate average pixel intensity
            var totalIntensity = 0
            val numPixels = grayscaleBitmap.width * grayscaleBitmap.height
            for (x in 0 until grayscaleBitmap.width) {
                for (y in 0 until grayscaleBitmap.height) {
                    val color = grayscaleBitmap.getPixel(x, y)
                    totalIntensity += Color.red(color)
                }
            }
            val avgIntensity = totalIntensity / numPixels

            // Calculate variance of pixel intensities
            var variance = 0.0
            for (x in 0 until grayscaleBitmap.width) {
                for (y in 0 until grayscaleBitmap.height) {
                    val color = grayscaleBitmap.getPixel(x, y)
                    val intensity = Color.red(color)
                    variance += Math.pow((intensity - avgIntensity).toDouble(), 2.0)
                }
            }
            variance /= numPixels.toDouble()

            Log.i("test" ,"glare variance ${variance}")

            return variance > threshold
        }

        /*** is blury old ***/

        fun isBlurry(bitmap: Bitmap, threshold:Double = 100.0): Boolean {
            val laplacianVariance = calculateLaplacianVariance(bitmap)
            // Check for low variance
//            Log.d("Test value for blur" , laplacianVariance.toString()+"   "+threshold.toString()+"     "+(laplacianVariance < threshold).toString())
            return laplacianVariance < threshold
        }

        fun calculateLaplacianVariance(image: Bitmap): Double {
            // Convert the image to grayscale
            val grayscaleImage = toGrayscale(image)

            // Calculate the Laplacian of the image
            val laplacian = calculateLaplacian(grayscaleImage)

            // Calculate the absolute value of each pixel in the Laplacian image
            val absoluteLaplacian = IntArray(laplacian.size)
            for (i in laplacian.indices) {
                absoluteLaplacian[i] = Math.abs(laplacian[i])
            }

            // Calculate the variance of the Laplacian image
            val mean = mean(absoluteLaplacian)
            var variance = 0.0
            for (i in absoluteLaplacian.indices) {
                variance += Math.pow(absoluteLaplacian[i] - mean, 2.0)
            }
            variance /= (absoluteLaplacian.size - 1).toDouble()
            return variance
        }

        fun toGrayscale(bmpOriginal: Bitmap): Bitmap {
            val bmpGrayscale = Bitmap.createBitmap(bmpOriginal.width, bmpOriginal.height, Bitmap.Config.RGB_565)
            val c = Canvas(bmpGrayscale)
            val paint = Paint()
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            val f = ColorMatrixColorFilter(cm)
            paint.colorFilter = f
            c.drawBitmap(bmpOriginal, 0f, 0f, paint)
            return bmpGrayscale
        }

        fun calculateLaplacian(image: Bitmap): IntArray {
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getPixels(pixels, 0, width, 0, 0, width, height)
            val laplacian = IntArray(width * height)
            for (i in 1 until height - 1) {
                for (j in 1 until width - 1) {
                    val pixel = pixels[i * width + j]
                    val left = pixels[i * width + j - 1]
                    val right = pixels[i * width + j + 1]
                    val top = pixels[(i - 1) * width + j]
                    val bottom = pixels[(i + 1) * width + j]
                    val sum = 4 * Color.red(pixel) - Color.red(left) - Color.red(right) - Color.red(top) - Color.red(bottom)
                    laplacian[i * width + j] = sum
                }
            }
            return laplacian
        }

        fun mean(values: IntArray): Double {
            var sum = 0.0
            for (value in values) {
                sum += value.toDouble()
            }
            return sum / values.size
        }
    }
}