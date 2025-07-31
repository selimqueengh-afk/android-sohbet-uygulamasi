package com.selimqueengh.sohbet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chatList: List<ChatItem>,
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameText: TextView = itemView.findViewById(R.id.textViewUsername)
        private val lastMessageText: TextView = itemView.findViewById(R.id.textViewLastMessage)
        private val timeText: TextView = itemView.findViewById(R.id.textViewTime)
        private val unreadCountText: TextView = itemView.findViewById(R.id.textViewUnreadCount)
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)

        fun bind(chatItem: ChatItem) {
            usernameText.text = chatItem.username
            lastMessageText.text = chatItem.lastMessage
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = timeFormat.format(Date(chatItem.timestamp))
            
            // Okunmamış mesaj sayısı
            if (chatItem.unreadCount > 0) {
                unreadCountText.text = chatItem.unreadCount.toString()
                unreadCountText.visibility = View.VISIBLE
            } else {
                unreadCountText.visibility = View.GONE
            }
            
            // Online durumu
            onlineIndicator.visibility = if (chatItem.isOnline) View.VISIBLE else View.GONE
            
            // Tıklama olayı
            itemView.setOnClickListener {
                onChatClick(chatItem)
            }
        }
    }
}