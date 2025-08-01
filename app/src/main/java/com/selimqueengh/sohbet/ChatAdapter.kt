package com.selimqueengh.sohbet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.models.ChatItem
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chatList: List<ChatItem>,
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val unreadCountText: TextView = itemView.findViewById(R.id.unreadCountText)
        val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        
        holder.usernameText.text = chat.username
        holder.lastMessageText.text = chat.lastMessage
        
        // Timestamp formatla
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(chat.timestamp)
        holder.timestampText.text = dateFormat.format(date)
        
        // Unread count
        if (chat.unreadCount > 0) {
            holder.unreadCountText.text = chat.unreadCount.toString()
            holder.unreadCountText.visibility = View.VISIBLE
        } else {
            holder.unreadCountText.visibility = View.GONE
        }
        
        // Online indicator
        holder.onlineIndicator.visibility = if (chat.isOnline) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener {
            onChatClick(chat)
        }
    }

    override fun getItemCount(): Int = chatList.size
}