package com.example.letslink.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.model.Group

//handles the ui where the group info will be displayed
//(Lackner, 2025a)
class GroupViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleTextView: TextView = itemView.findViewById(R.id.group_name_text)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.noteDescriptionTextView)
    private val inviteLinkTextView: TextView = itemView.findViewById(R.id.inviteLinkTextView)

    fun bind(
        group: Group,
        onGroupClick: (Group) -> Unit,

        ) {
        titleTextView.text = group.groupName
        descriptionTextView.text = group.description

        //  Display the invite link
        group.inviteLink?.let { link ->
            inviteLinkTextView.visibility = View.VISIBLE
            inviteLinkTextView.text = "Invite: $link"
        } ?: run {
            inviteLinkTextView.visibility = View.GONE
        }

        itemView.setOnClickListener {
            onGroupClick(group)
        }


    }
}

class GroupAdapter(
    private val onGroupClick: (Group) -> Unit,
    private val onGroupDelete: (Group) -> Unit
) : ListAdapter<Group, GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_card, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group, onGroupClick)
    }
}

class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
    override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem.groupId == newItem.groupId
    }

    override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem == newItem
    }
}