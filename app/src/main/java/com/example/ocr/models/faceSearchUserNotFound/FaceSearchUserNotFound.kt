package com.example.ocr.models.faceSearchUserNotFound

import com.google.gson.annotations.SerializedName

data class FaceSearchUserNotFound(

    @field:SerializedName("data")
	val data: UserImageData? = null,

    @field:SerializedName("success")
	val success: Boolean? = null,

    @field:SerializedName("message")
	val message: String? = null
)