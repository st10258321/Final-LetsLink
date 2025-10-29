package com.example.letslink.model

data class Invites(
    val groupId: String = "",
    val groupName: String = "",
    val description: String = "",
    val inviteLink: String = ""
)

data class InviteRequest(
    val groupId: String,
    val userId: String,
    val groupName: String,
    val description: String
)