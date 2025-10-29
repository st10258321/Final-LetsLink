package com.example.letslink.activities

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.letslink.model.Event
import com.example.letslink.model.EventVoting_m

class EventVoting : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val events = intent.getParcelableArrayListExtra<EventVoting_m>("events") ?: emptyList()
        val groupId = intent.getStringExtra("groupId") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""


        // Enable edge-to-edge display
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContent {
            EventVotingScreen(events,groupId,userId) // ← your Compose swipe deck
        }
    }
}
