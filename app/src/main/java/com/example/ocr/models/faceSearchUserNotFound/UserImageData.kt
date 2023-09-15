package com.example.ocr.models.faceSearchUserNotFound

import com.google.gson.annotations.SerializedName

data class UserImageData(

    @field:SerializedName("thumbImageUrl")
    val thumbImageUrl: String? = null,

	@field:SerializedName("imageUrl")
	val imageUrl: String? = null,

	@field:SerializedName("face-id")
	val faceId: String? = null
)