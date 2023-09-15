package com.example.ocr.models.userExists

import com.google.gson.annotations.SerializedName
import com.example.ocr.models.userExists.DataItem

data class ModelUserExists(

    @field:SerializedName("data")
	val data: List<DataItem?>? = null,

    @field:SerializedName("success")
	val success: Boolean? = null,

    @field:SerializedName("message")
	val message: String? = null
)