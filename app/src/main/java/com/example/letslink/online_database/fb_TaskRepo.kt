package com.example.letslink.online_database

import android.content.Context
import android.util.Log
import com.example.letslink.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.snapshots
import kotlinx.coroutines.tasks.await

class fb_TaskRepo(context: Context)  {
    private val db = FirebaseDatabase.getInstance().reference
    private var auth = FirebaseAuth.getInstance()
    fun createTask(task : Task, callback: (Boolean, String?) -> Unit){
        val user = auth.currentUser
        if(user != null){
            task.taskId = db.child("tasks").push().key ?: ""
            Log.d("__fb__--", "Task ID: ${task.taskId}")
            db.child("tasks").child(task.taskId).setValue(task)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        callback(true, "Task created successfully")
                    }else{
                        callback(false, "Failed to create task, please try again later")
                    }
                }
        }else{
            callback(false, "User not logged in")
        }
    }
    fun updateTaskStatus(task: Task) {
        val updates = mapOf<String, Any>("taskStatus" to task.taskStatus)

        db.child("tasks").child(task.taskId).updateChildren(updates).addOnCompleteListener {
            if(it.isSuccessful){
                Log.d("fb_TaskRepo", "Task status updated successfully")
            }else{
                Log.d("fb_TaskRepo", "Failed to update task status")
            }

        }
    }

     suspend fun getTasksForEvent(eventId: String) : List<Task> {
         var tasks = mutableListOf<Task>()
        val snapshot = db.child("tasks")
            .orderByChild("eventId")
            .equalTo(eventId)
            .get()
            .await()

        for(child in snapshot.children){
            val task = child.getValue(Task::class.java)
            if(task != null)
                tasks.add(task)
        }

        db.child("tasks")
            .orderByChild("eventId")
            .equalTo(eventId)
            .get()

         return tasks
    }
}