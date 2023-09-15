package com.example.ocr.repository.local

import androidx.room.*
import com.example.ocr.models.userLogin.User

@Dao
abstract class UsersDao {
    @Transaction
    open  fun deleteAndCreate(users: List<User?>?) {
        deleteAll()
        insertAll(users)
    }

    @Query("DELETE FROM " + DBConstant.USERS_TABLE_NAME)
    abstract fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(items: List<User?>?)

    @get:Query("SELECT * FROM " + DBConstant.USERS_TABLE_NAME)
    abstract val all: List<User?>?
    @Query("SELECT * FROM " + DBConstant.USERS_TABLE_NAME + " WHERE id =:id LIMIT 1")
    abstract fun getOne(id: Int): User?
    @Query("DELETE FROM " + DBConstant.USERS_TABLE_NAME)
    abstract fun nukeTable(): Int
    @Query("DELETE FROM " + DBConstant.USERS_TABLE_NAME + " WHERE id=:id")
    abstract fun deleteUser(id: Int): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(user: User?): Long
}