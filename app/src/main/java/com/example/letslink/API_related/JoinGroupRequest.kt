package com.example.letslink.API_related

import java.util.UUID

data class JoinGroupRequest(
    val groupId: String,
    val userId: UUID?
)