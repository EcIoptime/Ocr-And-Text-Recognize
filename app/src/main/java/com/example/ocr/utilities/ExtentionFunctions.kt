package com.example.ocr.utilities

import android.app.Activity
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.TypedValue
import com.google.android.material.snackbar.Snackbar

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics)

fun Activity.showSnackBar(msg: String) {
    //val parentLayout = findViewById<View>(android.R.id.content)
    Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
        .setAction("CLOSE") { }
        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
        .show()
}
fun String?.convertBooleanToInt():String{
    return if (this =="true")
        "1"
    else if (this =="false")
        "0"
    else
        "1"
}

fun String?.convertIntToBoolean():Boolean{
    return if (this =="1")
        true
    else if (this =="0")
        false
    else
        false
}

fun String?.isNullPassEmpty(): String {
    return if (this ==null)
        ""
    else if (this =="null")
        ""
    else
        this
}

fun String.appendPlusSign(): String {
    return "+${this.replace("+", "")}"
}

fun Bitmap.rotateImage(angle: Float): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun Bitmap.getHeightOfWidth(maxProportion: Double): Bitmap {

    var newBitmap: Bitmap = this
//        if (newBitmap.width < 500){
    val scaledHeight = Math.ceil(maxProportion * (this.height.toFloat() / this.width.toFloat())).toInt()
//                val scaledWidth = Math.ceil(scaledHeight.toDouble() * it.getWidth() / it.getHeight()).toInt()
    newBitmap = Bitmap.createScaledBitmap(this, maxProportion.toInt(), scaledHeight.toInt(), true)
//        }

    if (newBitmap.height < 500) {
        val scaledWidth = Math.ceil(maxProportion.toDouble() * (this.width / this.height.toFloat())).toInt()
        newBitmap = Bitmap.createScaledBitmap(this, scaledWidth, maxProportion.toInt(), true)
    }
//        val scaledHeight = Math.ceil(maxWidth * it.height / it.width).toInt()
////                val scaledWidth = Math.ceil(scaledHeight.toDouble() * it.getWidth() / it.getHeight()).toInt()
//        val newBitmap = Bitmap.createScaledBitmap(it, maxWidth.toInt(), scaledHeight.toInt(), true)
    return newBitmap
}