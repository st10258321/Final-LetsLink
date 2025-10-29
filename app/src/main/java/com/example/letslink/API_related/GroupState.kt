package com.example.api_test

import java.util.UUID

data class GroupState (
    val userId : String = "",
    val groupName :String ="",
    val description: String ="",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val inviteLink: String? = null
)