package com.example.ocr.models.faceSearch

import com.google.gson.annotations.SerializedName

data class Metadata(

    @field:SerializedName("headers")
	val headers: Headers? = null,

    @field:SerializedName("transferStats")
	val transferStats: TransferStats? = null,

    @field:SerializedName("statusCode")
	val statusCode: Int? = null,

    @field:SerializedName("effectiveUri")
	val effectiveUri: String? = null
)