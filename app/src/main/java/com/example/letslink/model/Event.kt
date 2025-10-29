package com.example.letslink.model

data class Event(
    var eventId : String = "",
    var ownerId : String = "",
    var title: String= "",
    var description: String= "",
    var location: String= "",
    var startTime: String= "",
    var endTime: String= "",
    var date: String= "",
    var groups: List<String>? = emptyList()
)