package com.example.paddleocrlib

import android.app.Application
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite

class AppControllerPaddleOcr : Application() {

    var isTfliteInitialized: Boolean = false
    val initializeTask: Task<Void> by lazy {
        TfLite.initialize(
            this,
            TfLiteInitializationOptions.builder()
                .setEnableGpuDelegateSupport(false)
                .build()
        ).continueWithTask { task ->
            if (task.isSuccessful) {
                isTfliteInitialized = true
                return@continueWithTask Tasks.forResult(null)
            } else {
                // Fallback to initialize interpreter without GPU
                return@continueWithTask TfLite.initialize(this)
            }
        }
            .addOnFailureListener {
                isTfliteInitialized = false
            }
    }

    override fun onCreate() {
        super.onCreate()
    }
}