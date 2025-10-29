package com.example.letslink.viewmodels

import androidx.lifecycle.ViewModel
import com.example.letslink.online_database.fb_EventsRepo

class EventViewModel(private val repo: fb_EventsRepo) : ViewModel() {
//    fun createEvent(title:String, description:String, location:String, startTime:String, endTime:String, date:String) : String  {
//        repo.createEvent(title,description,location,startTime,endTime,date) { isComplete ->
//            if (isComplete) {
//                return@createEvent "Event created successfully"
//            }
//
//
//        }
//        return false
//    }
}