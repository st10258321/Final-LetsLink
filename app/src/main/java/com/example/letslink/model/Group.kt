package com.example.letslink.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "Groups",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("userId"),
            childColumns = arrayOf("userId"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Group(
    @PrimaryKey
    val groupId: UUID = UUID.randomUUID(),

    val userId: UUID?,

    val groupName: String,

    val description: String,

    val inviteLink: String? = null
)