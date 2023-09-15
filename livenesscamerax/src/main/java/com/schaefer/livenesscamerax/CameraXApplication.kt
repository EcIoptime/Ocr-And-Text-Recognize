package com.schaefer.livenesscamerax

import android.app.Application


internal class CameraXApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }

    private fun initializeTimber() {
//        if (BuildConfig.DEBUG) {
//            Timber.plant(DebugTree())
//        }
    }
}
