package com.example.letslink.models

data class ChatMessage(
    val isSent: Boolean,   // true = user, false = received
    val text: String,
    val time: String
)