package com.example.letslink.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.activities.GroupEventActivity
import com.example.letslink.R
import com.example.letslink.model.EventVoting_m
import de.hdodenhof.circleimageview.CircleImageView

class GroupEventAdapter(
    private val context: Context,
    private val eventList: List<EventVoting_m>
) : RecyclerView.Adapter<GroupEventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventCard: CardView = itemView.findViewById(R.id.event_card)
       // val eventImage: CircleImageView = itemView.findViewById(R.id.group_profile_image)
        var eventName: TextView = itemView.findViewById(R.id.event_name_text)
        var groupAssociated: TextView = itemView.findViewById(R.id.group_assotiated)
        val groupEventsText: TextView = itemView.findViewById(R.id.group_events_text)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrow_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_events_card, parent, false) // replace with your actual layout name
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        holder.eventName.text = event.title

        // Click listener to open GroupEventActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, GroupEventActivity::class.java)
            intent.putExtra("event_data", event.eventId) // event is Parcelable
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = eventList.size
}
