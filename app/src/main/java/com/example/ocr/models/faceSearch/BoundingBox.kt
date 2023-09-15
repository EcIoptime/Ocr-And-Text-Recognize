package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName

data class BoundingBox(

	@field:SerializedName("Left")
	val left: Double? = null,

	@field:SerializedName("Top")
	val top: Double? = null,

	@field:SerializedName("Height")
	val height: Double? = null,

	@field:SerializedName("Width")
	val width: Double? = null
)