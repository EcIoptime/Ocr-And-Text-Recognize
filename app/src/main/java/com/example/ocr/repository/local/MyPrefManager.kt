package com.example.ocr.repository.local

import android.content.Context
import android.content.SharedPreferences
import com.example.ocr.models.userLogin.ModelLogin
import com.iopime.telemedicines.models.userRegistration.UserRegistration
import com.example.ocr.utilities.LoginType
import hu.autsoft.krate.Krate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.gson.gsonPref
import hu.autsoft.krate.stringPref

class MyPrefManager(context: Context) : Krate {
    override val sharedPreferences: SharedPreferences = context.applicationContext.getSharedPreferences("PrefCustom", Context.MODE_PRIVATE)
    var isIntroShown by booleanPref("isIntroShown", false)
    var isRemember by booleanPref("isRemember", false)
    var isLogin by booleanPref("isLogin", false)
    var dontShowIntro by booleanPref("dontShowIntro", false)
    var modelLogin: ModelLogin? by gsonPref("modelLogin")
    var normalUserToken by stringPref("normalUserToken","")
    var loginType by stringPref("loginType", LoginType.NONE.name )
    var currentUser: UserRegistration? by gsonPref("currentUser")
    var currentStaff: UserRegistration? by gsonPref("currentStaff")

    var mrzModelPath by stringPref("mrzModelPath", "" )

    var modelMrzPath by stringPref("modelMrzPath", "" )
    var modelKenyaPassportPath by stringPref("modelKenyaPassportPath", "" )

    var modelKenyaIdCardFrontPath by stringPref("modelKenyaIdCardPathFront", "" )
    var modelKenyaIdCardBackPath by stringPref("modelKenyaIdCardBackPath", "" )

    var modelKenyaIdCardPath by stringPref("modelKenyaIdCardPath", "" )
    var modelKenyaLicensePath by stringPref("modelKenyaLicensePath", "" )
    var modelSaPassportPath by stringPref("modelSaPassportPath", "" )
    var modelSaIdCardPath by stringPref("modelSaIdCardPath", "" )
    var modelSaLicensePath by stringPref("modelSaLicensePath", "" )
    var modelZimbabwePassportPath by stringPref("modelZimbabwePassportPath", "" )
    var modelZimbabweIdCardPath by stringPref("modelZimbabweIdCardPath", "" )
    var modelZimbabweLicenseCardPath by stringPref("modelZimbabweLicenseCardPath", "" )

    var modelAllCardPath by stringPref("modelAllCardPath", "" )


}