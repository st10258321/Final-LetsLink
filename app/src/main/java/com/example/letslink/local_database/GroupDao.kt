package com.example.letslink.local_database


import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.letslink.model.Group
import com.example.letslink.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GroupDao {

    //handles all database operations
    @Upsert
    suspend fun insertGroup(groups: Group)

    @Query("SELECT userId FROM `Groups` WHERE groupId = :groupId")
    suspend fun getUserIdByNoteId(groupId: UUID): UUID?
    // Get group of a user
    @Query("SELECT * FROM `Groups` WHERE userId = :userId")
    fun getNotesByUserId(userId: String): Flow<List<Group>>

    // To get user details along with their notes
    @Query("SELECT * FROM User WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM `Groups` WHERE groupId = :groupId")
    fun getNoteById(groupId: String): Flow<List<Group>>

    @Query("SELECT * FROM `Groups` WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): Group?

    @Query("SELECT * FROM `Groups` WHERE userId = :userId ORDER BY groupId DESC")
    fun getNotesByUserIdDesc(userId: String): Flow<List<Group>>
}