package com.example.letslink.online_database

import android.content.Context
import android.util.Log
import com.example.letslink.R
import com.example.letslink.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.snapshots
import kotlinx.coroutines.tasks.await
import kotlin.context


class fb_TaskRepo(private val context: Context)  {
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
                        callback(true, context.getString(R.string.ctp9_task_created_successfully))
                    }else{
                        callback(false, context.getString(R.string.ctp9_failed_to_create_task))
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