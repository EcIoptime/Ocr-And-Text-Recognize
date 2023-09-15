package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName

data class ImageData(

    @field:SerializedName("SearchedFaceConfidence")
	val searchedFaceConfidence: Double? = null,

    @field:SerializedName("@metadata")
	val metadata: Metadata? = null,

    @field:SerializedName("FaceMatches")
	val faceMatches: List<FaceMatchesItem?>? = null,

    @field:SerializedName("SearchedFaceBoundingBox")
	val searchedFaceBoundingBox: SearchedFaceBoundingBox? = null,

    @field:SerializedName("FaceModelVersion")
	val faceModelVersion: String? = null
)