package com.example.letslink.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.model.EventVotingResults

class EventResultsAdapter(
    private val context: Context,
    private var eventResults: List<EventVotingResults>
) : RecyclerView.Adapter<EventResultsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtEventTitle: TextView = itemView.findViewById(R.id.txt_event_title)
        val txtYesVotes: TextView = itemView.findViewById(R.id.txt_yes_votes)
        val txtNoVotes: TextView = itemView.findViewById(R.id.txt_no_votes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_voting_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = eventResults[position]
        holder.txtEventTitle.text = result.eventTitle
        holder.txtYesVotes.text = "Yes: ${result.yesCount}"
        holder.txtNoVotes.text = "No: ${result.noCont}"
    }

    override fun getItemCount(): Int = eventResults.size

    fun updateData(newResults: List<EventVotingResults>) {
        eventResults = newResults
        notifyDataSetChanged()
    }
}
