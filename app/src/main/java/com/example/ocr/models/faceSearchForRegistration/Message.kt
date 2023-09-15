package com.example.ocr.models.faceSearchForRegistration

import com.google.gson.annotations.SerializedName

data class Message(

	@field:SerializedName("user")
	val user: User? = null
)