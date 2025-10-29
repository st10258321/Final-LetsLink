package com.example.letslink.model

data class Task (
    var taskId: String = "",
    var eventId: String = "",
    var taskName: String = "",
    var taskDescription: String = "",
    var taskDuration: String = "",
    var dueDate: String = "",
    var taskStatus: String = "pending",
)
