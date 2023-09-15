package com.example.ocr.models.fingerData

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_pattern")
data class FingerPatterns(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "fileName") val fileName: String,
    @ColumnInfo(name = "versionImage") val versionImage: Int,
    @ColumnInfo(name = "templateData", typeAffinity = ColumnInfo.BLOB) val data: ByteArray
)