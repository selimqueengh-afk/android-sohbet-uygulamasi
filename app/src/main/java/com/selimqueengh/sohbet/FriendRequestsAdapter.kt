package com.selimqueengh.sohbet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FriendRequestsAdapter(
    private val requests: List<FriendRequest>,
    private val onActionClick: (FriendRequest, String) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        
        holder.usernameText.text = request.senderUsername
        
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = Date(request.timestamp)
        holder.timeText.text = dateFormat.format(date)
        
        holder.acceptButton.setOnClickListener {
            onActionClick(request, "accept")
        }
        
        holder.rejectButton.setOnClickListener {
            onActionClick(request, "reject")
        }
    }

    override fun getItemCount() = requests.size
}