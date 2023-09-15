package com.example.ocr.models.fcmNotification

import com.google.gson.annotations.SerializedName

data class FcmNotification(
    @SerializedName("data")
    val data: Map<String, String>,

    @SerializedName("to")
    var topic: String,
)


enum class FCMTopics { NEW_ADDED, DELETE_USER, UPDATE_USER, LOGOUT_USER }