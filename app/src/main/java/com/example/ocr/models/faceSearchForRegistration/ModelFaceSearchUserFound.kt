package com.example.ocr.models.faceSearchForRegistration

import com.example.ocr.models.faceSearchForRegistration.Message
import com.google.gson.annotations.SerializedName

data class ModelFaceSearchUserFound(

	@field:SerializedName("data")
	val data: String? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: Message? = null
)