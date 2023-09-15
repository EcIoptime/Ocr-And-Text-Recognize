package com.example.ocr.common

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import androidx.room.Room
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.ocr.R
import com.example.ocr.repository.local.DBConstant
import com.example.ocr.repository.local.LocalRepository
import com.iopime.telemedicines.models.userRegistration.UserRegistration
import com.example.ocr.repository.Repository
import com.example.ocr.repository.local.LocalDB
import com.example.ocr.repository.local.MyPrefManager
import com.example.ocr.repository.remote.RemoteRepository

import com.example.ocr.models.cardScanningDetail.Attribute
import kotlin.properties.Delegates

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()
        myPrefManager = MyPrefManager(this)
        instance = this
        val remoteRepository = RemoteRepository()
        val localDB: LocalDB = Room
            .databaseBuilder(applicationContext, LocalDB::class.java, DBConstant.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
        localRepository = LocalRepository(localDB, myPrefManager)
        repository = Repository(remoteRepository, localRepository)
        if (!Python.isStarted())
            Python.start(AndroidPlatform(this))

        // Acquire a wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Telemedicine::MyWakeLock")
            wakeLock?.acquire()
        } catch (e: Exception) {
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    override fun onTerminate() {
        // Release the wake lock
        wakeLock?.release()
        super.onTerminate()

        // Perform cleanup tasks here
    }

    companion object {

        var callBackProgress: ((Int) -> Unit)? = null
        var isDbDownloading: Int? by Delegates.observable(null) { property, oldValue, newValue ->
            newValue?.let { callBackProgress?.invoke(it) }
        }

        fun getProgressBack(callBackProgress: ((Int?) -> Unit)) {
            Companion.callBackProgress = callBackProgress
        }

        var downloadingRegulaDB = false

        var attributes: MutableList<Attribute>? = null

        @get:Synchronized
        var instance: AppController? = null

        @get:Synchronized
        var myPrefManager: MyPrefManager? = null

        @get:Synchronized
        var repository: Repository? = null
        private var localRepository: LocalRepository? = null
        var isSocialLogin = false


        var userModel: UserRegistration? = null
    }
}