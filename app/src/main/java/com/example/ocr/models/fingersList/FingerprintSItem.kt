package com.example.ocr.models.fingersList

import com.google.gson.annotations.SerializedName

data class FingerprintSItem(

	@field:SerializedName("image")
	val image: String? = null,

	@field:SerializedName("name_text")
	val nameText: String? = null
)