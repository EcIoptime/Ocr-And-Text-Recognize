package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName
import com.example.ocr.models.faceSearch.Face

data class FaceMatchesItem(

	@field:SerializedName("Similarity")
	val similarity: Double? = null,

	@field:SerializedName("Face")
	val face: Face? = null
)