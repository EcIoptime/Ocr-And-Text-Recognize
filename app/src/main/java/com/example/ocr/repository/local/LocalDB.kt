package com.example.ocr.repository.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ocr.models.fingerData.EntityDao
import com.example.ocr.models.fingerData.FingerPatterns
import com.example.ocr.models.userLogin.User

@Database(entities = [User::class, FingerPatterns::class], version = 3, exportSchema = false)
abstract class LocalDB : RoomDatabase() {
    abstract fun usersDao(): UsersDao?
    abstract fun entityDao(): EntityDao?
}