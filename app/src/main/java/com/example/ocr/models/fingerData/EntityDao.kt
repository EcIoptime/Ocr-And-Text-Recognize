package com.example.ocr.models.fingerData

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
abstract class EntityDao {

    @Insert
    abstract suspend fun insert(entity: FingerPatterns)

    @Query("SELECT *  FROM table_pattern WHERE id = :id")
    abstract suspend fun getById(id: Int): FingerPatterns

    @Query("SELECT *  FROM table_pattern WHERE fileName = :id")
    abstract suspend fun getByFileName(id: String): FingerPatterns

    @Query("DELETE  FROM table_pattern WHERE fileName = :fileName")
    abstract suspend fun deleteById(fileName: String)

    @Query("SELECT *  FROM table_pattern ")
    abstract suspend fun getByAll(): List<FingerPatterns>

}