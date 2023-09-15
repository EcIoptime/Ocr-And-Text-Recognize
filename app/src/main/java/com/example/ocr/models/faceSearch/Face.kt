package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName
import com.example.ocr.models.faceSearch.BoundingBox

data class Face(

    @field:SerializedName("FaceId")
	val faceId: String? = null,

    @field:SerializedName("IndexFacesModelVersion")
	val indexFacesModelVersion: String? = null,

    @field:SerializedName("Confidence")
	val confidence: Double? = null,

    @field:SerializedName("BoundingBox")
	val boundingBox: BoundingBox? = null,

    @field:SerializedName("ImageId")
	val imageId: String? = null
)