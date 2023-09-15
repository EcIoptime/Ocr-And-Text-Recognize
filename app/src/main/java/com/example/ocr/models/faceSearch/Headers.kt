package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName

data class Headers(

	@field:SerializedName("date")
	val date: String? = null,

	@field:SerializedName("content-length")
	val contentLength: String? = null,

	@field:SerializedName("x-amzn-requestid")
	val xAmznRequestid: String? = null,

	@field:SerializedName("content-type")
	val contentType: String? = null
)