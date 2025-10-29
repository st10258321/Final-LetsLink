package com.example.letslink.local_database


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.example.letslink.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM User WHERE firstName = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM User WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?



}