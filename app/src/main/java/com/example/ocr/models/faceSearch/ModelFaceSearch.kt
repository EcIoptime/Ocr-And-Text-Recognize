package com.example.ocr.models.faceSearch

import com.example.ocr.models.faceSearch.Data
import com.google.gson.annotations.SerializedName

data class ModelFaceSearch(

    @field:SerializedName("data")
	val data: Data? = null,

    @field:SerializedName("success")
	val success: Boolean? = null,

    @field:SerializedName("message")
	val message: List<Any?>? = null
)