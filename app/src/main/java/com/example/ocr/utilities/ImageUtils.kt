package com.example.ocr.utilities

import android.graphics.*
import kotlin.math.sqrt

class ImageUtils {

    companion object{
        private fun bitmapToGrayscaleArray(bitmap: Bitmap): Array<IntArray>? {
            val width = bitmap.width
            val height = bitmap.height
            val grayscaleArray = Array(width) { IntArray(height) }
            for (i in 0 until width) {
                for (j in 0 until height) {
                    val pixel = bitmap.getPixel(i, j)
                    val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                    grayscaleArray[i][j] = gray
                }
            }
            return grayscaleArray
        }
        private fun calculateVarianceOfLaplacian(grayscaleArray: Array<IntArray>): Double {
            val width = grayscaleArray.size
            val height = grayscaleArray[0].size
            var sumLaplacian = 0.0
            var sumSquaredLaplacian = 0.0
            for (i in 1 until width - 1) {
                for (j in 1 until height - 1) {
                    val laplacian = -grayscaleArray[i - 1][j] - grayscaleArray[i + 1][j] - grayscaleArray[i][j - 1] - grayscaleArray[i][j + 1] + 4 * grayscaleArray[i][j]
                    sumLaplacian += laplacian.toDouble()
                    sumSquaredLaplacian += (laplacian * laplacian).toDouble()
                }
            }
            val numPixels = (width - 2) * (height - 2)
            val meanLaplacian = sumLaplacian / numPixels
            val meanSquaredLaplacian = sumSquaredLaplacian / numPixels
            return meanSquaredLaplacian - meanLaplacian * meanLaplacian
        }

        /*** calculate bluriness ***/
        fun isBlurry(bitmap: Bitmap?, threshold:Double = 100.0): Boolean {
            val grayscaleArray = bitmapToGrayscaleArray(bitmap!!)
            return calculateVarianceOfLaplacian(grayscaleArray!!) < threshold
        }

        /*** check image for glare ***/
        @JvmStatic
        fun hasGlare(bitmap: Bitmap, threshold:Double = 1000.0  ): Double {
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
            return variance// > threshold
        }

        fun hasGlare(bitmap: Bitmap, threshold: Int = 240, minGlarePixels: Int = 10): Boolean {
            val grayBitmap = toGrayscale(bitmap)
            val width = grayBitmap.width
            val height = grayBitmap.height

            var glarePixelCount = 0
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = grayBitmap.getPixel(x, y)
                    val intensity = Color.red(pixel)
                    if (intensity >= threshold) {
                        glarePixelCount++
                        if (glarePixelCount >= minGlarePixels) {
                            return true
                        }
                    }
                }
            }

            return false
        }


        fun toGrayscale(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(grayBitmap)
            val paint = Paint()
            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    0.299f, 0.587f, 0.114f, 0f, 0f,
                    0.299f, 0.587f, 0.114f, 0f, 0f,
                    0.299f, 0.587f, 0.114f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return grayBitmap
        }

        fun getVariance(bitmap: Bitmap): Double {
            val width = bitmap.width
            val height = bitmap.height
            var sum = 0.0
            var sumSquared = 0.0

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val intensity = Color.red(pixel).toDouble()
                    sum += intensity
                    sumSquared += intensity * intensity
                }
            }

            val pixelCount = (width * height).toDouble()
            val mean = sum / pixelCount
            return (sumSquared / pixelCount) - (mean * mean)
        }

        fun applySobelOperator(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val sobelBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 1 until width - 1) {
                for (y in 1 until height - 1) {
                    val gx = (Color.red(bitmap.getPixel(x + 1, y - 1)) + 2 * Color.red(bitmap.getPixel(x + 1, y)) + Color.red(
                        bitmap.getPixel(x + 1, y + 1)
                    )) - (Color.red(bitmap.getPixel(x - 1, y - 1)) + 2 * Color.red(bitmap.getPixel(x - 1, y)) + Color.red(
                        bitmap.getPixel(x - 1, y + 1)
                    ))
                    val gy = (Color.red(bitmap.getPixel(x - 1, y + 1)) + 2 * Color.red(bitmap.getPixel(x, y + 1)) + Color.red(
                        bitmap.getPixel(x + 1, y + 1)
                    )) - (Color.red(bitmap.getPixel(x - 1, y - 1)) + 2 * Color.red(bitmap.getPixel(x, y - 1)) + Color.red(bitmap.getPixel(x + 1, y - 1)))
                    val gradientMagnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt()
                    val edgeColor = Color.argb(255, gradientMagnitude, gradientMagnitude, gradientMagnitude)
                    sobelBitmap.setPixel(x, y, edgeColor)
                }
            }

            return sobelBitmap

        }

        fun containsCard(bitmap: Bitmap,  edgeCountThreshold: Int = 1000): Boolean {

            val grayBitmap = toGrayscale(bitmap)
            val sobelBitmap = applySobelOperator(grayBitmap)
            var edgeCount = 0
            for (x in 0 until sobelBitmap.width) {
                for (y in 0 until sobelBitmap.height) {
                    if (Color.red(sobelBitmap.getPixel(x, y)) > 128) {
                        edgeCount++
                    }
                }
            }

            return edgeCount > edgeCountThreshold
        }


    }
}