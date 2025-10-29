package com.example.letslink.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.model.Invites
//handles the ui where the group invites will be displayed
//(Lackner, 2025a)
class JoinViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleTextView: TextView = itemView.findViewById(R.id.group_name_text)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.noteDescriptionTextView)
    private val joinButton: Button = itemView.findViewById(R.id.btn_Join)
    private val inviteLinkTextView: TextView = itemView.findViewById(R.id.inviteLinkTextView)

    fun bind(
        invite: Invites, // Bind with ReceivedInvites
        onJoinClick: (Invites) -> Unit
    ) {
        titleTextView.text = invite.groupName
        descriptionTextView.text = invite.description
        // Display the link
        inviteLinkTextView.text = invite.inviteLink

        // Join button click
        joinButton.setOnClickListener {
            onJoinClick(invite) // Pass the specific invite object back
        }
    }
}


class JoinAdapter(
    private val onJoinClick: (Invites) -> Unit
) : ListAdapter<Invites, JoinViewHolder>(JoinDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JoinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_link, parent, false)
        return JoinViewHolder(view)
    }

    override fun onBindViewHolder(holder: JoinViewHolder, position: Int) {
        val invite = getItem(position) //gets the invites postions

        holder.bind(invite, onJoinClick)
    }
}

class JoinDiffCallback : DiffUtil.ItemCallback<Invites>() {
    override fun areItemsTheSame(oldItem: Invites, newItem: Invites): Boolean {
        // Items are the same if the groupId
        return oldItem.groupId == newItem.groupId
    }

    override fun areContentsTheSame(oldItem: Invites, newItem: Invites): Boolean {
        return oldItem == newItem
    }
}