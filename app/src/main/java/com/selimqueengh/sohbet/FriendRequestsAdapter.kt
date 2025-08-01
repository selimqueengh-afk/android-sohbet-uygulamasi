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
    private val requestsList: List<FriendRequest>,
    private val onActionClick: (FriendRequest, String) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestsList[position]
        
        holder.usernameText.text = request.senderUsername
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = Date(request.timestamp)
        holder.timestampText.text = dateFormat.format(date)
        
        holder.acceptButton.setOnClickListener {
            onActionClick(request, "accept")
        }
        
        holder.rejectButton.setOnClickListener {
            onActionClick(request, "reject")
        }
    }

    override fun getItemCount(): Int = requestsList.size
}