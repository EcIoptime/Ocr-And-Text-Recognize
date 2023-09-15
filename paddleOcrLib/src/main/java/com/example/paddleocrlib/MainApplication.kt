package com.example.paddleocrlib

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig


class MainApplication: Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()

    }

    override fun getCameraXConfig(): CameraXConfig {


        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }
    companion object{

//        var loadingBar: KProgressHUD? = null
        fun showLoading(context: Context)
      {
//             loadingBar = KProgressHUD.create(context)
//              .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
//              .setLabel("Please wait while loading")
//              .setCancellable(true)
//              .setAnimationSpeed(2)
//              .setDimAmount(0.5f)
//              .show();
      }


        fun  hideLoading()
        {
//            if (loadingBar != null )
//            {
//                if (loadingBar?.isShowing ==true)
//                {
//                    loadingBar!!.dismiss()
//
//                }
//            }
        }

        fun  checkLoadingBarShowing():Boolean
        {
//            if (loadingBar != null )
//            {
//                if (loadingBar!!.isShowing)
//                {
//                    return true
//
//                }
//            }
            return false
        }



    }
}