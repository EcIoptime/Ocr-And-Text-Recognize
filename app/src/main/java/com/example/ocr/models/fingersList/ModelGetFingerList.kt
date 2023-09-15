package com.example.ocr.models.fingersList

import com.example.ocr.models.fingersList.FingerprintSItem
import com.google.gson.annotations.SerializedName

data class ModelGetFingerList(

	@field:SerializedName("STATUS")
	val sTATUS: String? = null,

	@field:SerializedName("FingerprintS")
	val fingerprintS: List<FingerprintSItem?>? = null
)