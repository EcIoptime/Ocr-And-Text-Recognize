package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName
import com.iopime.telemedicines.models.userRegistration.UserRegistration

data class Data(

    @field:SerializedName("image-data")
	val imageData: ImageData? = null,

    @field:SerializedName("user")
	var user: UserRegistration? = null
)