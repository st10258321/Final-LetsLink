// GroupChatActivity.kt
package com.example.letslink.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.adapters.MessagesAdapter
import com.example.letslink.models.ChatMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ImageButton
import java.text.SimpleDateFormat
import java.util.*

class GroupChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMenu: LinearLayout

    private var isFabOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_group_chat_u_i) // your layout

        recyclerView = findViewById(R.id.messages_recycler)
        messageInput = findViewById(R.id.message_inputt)
        sendButton = findViewById(R.id.send_button)
        fabMain = findViewById(R.id.fab_main)
        fabMenu = findViewById(R.id.fab_menu)

        adapter = MessagesAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Send button
        sendButton.setOnClickListener {
            val text = messageInput.text.toString()
            if (text.isNotEmpty()) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val message = ChatMessage(true, text, time)
                adapter.addMessage(message)
                recyclerView.scrollToPosition(adapter.itemCount - 1)
                messageInput.text.clear()
            } else {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            }
        }

        // FAB toggle
        fabMain.setOnClickListener {
            if (isFabOpen) {
                fabMenu.visibility = LinearLayout.GONE
                fabMain.setImageResource(R.drawable.ic_add)
            } else {
                fabMenu.visibility = LinearLayout.VISIBLE
                fabMain.setImageResource(R.drawable.ic_close)
            }
            isFabOpen = !isFabOpen
        }
    }
}
