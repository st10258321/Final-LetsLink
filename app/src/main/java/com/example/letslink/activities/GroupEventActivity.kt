package com.example.letslink.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.letslink.R
import com.example.letslink.online_database.fb_EventsRepo
import com.example.letslink.online_database.fb_TaskRepo
import kotlinx.coroutines.launch
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.adapter.EventTaskAdapter
import com.example.letslink.adapter.TaskAdapter

class GroupEventActivity : AppCompatActivity() {
    private lateinit var fbEventsrepo: fb_EventsRepo
    private lateinit var taskRepo : fb_TaskRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_event)


        val eventId = intent.getStringExtra("event_data")

        fbEventsrepo = fb_EventsRepo(this)
        taskRepo = fb_TaskRepo(this)

        val tasksRV = findViewById<RecyclerView>(R.id.tasksRV)
        tasksRV.layoutManager = LinearLayoutManager(this)
        tasksRV.adapter = EventTaskAdapter(this, mutableListOf(),{})



        lifecycleScope.launch{
            val event = fbEventsrepo.getEventById(eventId!!)
            if(event != null) {
                val eventName = findViewById<TextView>(R.id.eventName)
                val eventDescription = findViewById<TextView>(R.id.eventDescription)
                val eventLocation = findViewById<TextView>(R.id.eventLocation)
                val eventTime = findViewById<TextView>(R.id.eventTime)
                val tasks = taskRepo.getTasksForEvent(eventId)
                Log.d("tasks","${tasks.size}")
                if (tasks.isNotEmpty()){
                    val adapter = EventTaskAdapter(this@GroupEventActivity, tasks, { task ->
                        taskRepo.updateTaskStatus(task)
                    })
                    tasksRV.adapter = adapter
                }
                eventName.text = event.title
                eventDescription.text = event.description
                eventLocation.text = event.location
                eventTime.text = "${event.startTime} - ${event.endTime}"



            }else
                Log.d("fb_EventsRepo", "Event not found")
        }
    }
}