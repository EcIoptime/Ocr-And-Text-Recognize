package com.example.paddleocrlib

import android.content.Context
import android.content.SharedPreferences
import hu.autsoft.krate.Krate
import hu.autsoft.krate.stringPref

class MyPrefManager(var context: Context) : Krate {


    override val sharedPreferences: SharedPreferences = context.applicationContext.getSharedPreferences("PrefCustom", Context.MODE_PRIVATE)
    var mrzModelPath by stringPref("mrzModelPath", "" )

    var modelMrzPath by stringPref("modelMrzPath", "" )
    var modelKenyaPassportPath by stringPref("modelKenyaPassportPath", "" )

    var modelKenyaIdCardFrontPath by stringPref("modelKenyaIdCardPathFront", "" )
    var modelKenyaIdCardBackPath by stringPref("modelKenyaIdCardBackPath", "" )

    var modelKenyaLicensePath by stringPref("modelKenyaLicensePath", "" )
    var modelSaPassportPath by stringPref("modelSaPassportPath", "" )
    var modelSaIdCardPath by stringPref("modelSaIdCardPath", "" )
    var modelSaLicensePath by stringPref("modelSaLicensePath", "" )
    var modelZimbabwePassportPath by stringPref("modelZimbabwePassportPath", "" )
    var modelZimbabweIdCardPath by stringPref("modelZimbabweIdCardPath", "" )
    var modelZimbabweLicenseCardPath by stringPref("modelZimbabweLicenseCardPath", "" )

    var modelAllCardPath by stringPref("modelAllCardPath", "" )

    companion object{
        private var instance:MyPrefManager? =null
        @Synchronized
        fun  getInstance(context:Context): MyPrefManager? {
            if (instance ==null){
                instance = MyPrefManager(context = context)
            }
            return instance
        }
    }

}