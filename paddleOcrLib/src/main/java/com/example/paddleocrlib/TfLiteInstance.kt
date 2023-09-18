package com.example.paddleocrlib

import android.app.Application
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite

class TfLiteInstance private constructor() {
    init {
        // Initialization code, if any
    }

    companion object {
        private var instance: TfLiteInstance? = null
        private var context: Application? = null

        fun getInstance(): TfLiteInstance {
            if (instance == null) {
                instance = TfLiteInstance()
            }
            return instance!!
        }


        var isTfliteInitialized: Boolean = false
        val initializeTask: Task<Void> by lazy {
            TfLite.initialize(
                context,
                TfLiteInitializationOptions.builder()
                    .setEnableGpuDelegateSupport(false)
                    .build()
            ).continueWithTask { task ->
                if (task.isSuccessful) {
                    isTfliteInitialized = true
                    return@continueWithTask Tasks.forResult(null)
                } else {
                    // Fallback to initialize interpreter without GPU
                    return@continueWithTask TfLite.initialize(context)
                }
            }
                .addOnFailureListener {
                    isTfliteInitialized = false
                }
        }
    }

}