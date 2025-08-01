package com.selimqueengh.sohbet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.models.Friend

class FriendsAdapter(
    private val friends: List<Friend>,
    private val onFriendClick: (Friend) -> Unit,
    private val onFriendDelete: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.friendNameText)
        val statusText: TextView = itemView.findViewById(R.id.friendStatusText)
        val deleteButton: android.widget.ImageButton = itemView.findViewById(R.id.deleteFriendButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        
        holder.nameText.text = friend.name
        
        // Online/offline durumunu göster
        val statusText = if (friend.isOnline) {
            "🟢 Çevrimiçi"
        } else {
            // Son görülme zamanını hesapla
            val lastSeenText = when (val lastSeen = friend.lastSeen) {
                is com.google.firebase.Timestamp -> {
                    val timeDiff = System.currentTimeMillis() - lastSeen.toDate().time
                    when {
                        timeDiff < 60000 -> "🟡 Az önce" // 1 dakika
                        timeDiff < 3600000 -> "🟡 ${timeDiff / 60000} dakika önce" // 1 saat
                        timeDiff < 86400000 -> "🟡 ${timeDiff / 3600000} saat önce" // 1 gün
                        else -> "⚫ ${timeDiff / 86400000} gün önce"
                    }
                }
                is Long -> {
                    val timeDiff = System.currentTimeMillis() - lastSeen
                    when {
                        timeDiff < 60000 -> "🟡 Az önce"
                        timeDiff < 3600000 -> "🟡 ${timeDiff / 60000} dakika önce"
                        timeDiff < 86400000 -> "🟡 ${timeDiff / 3600000} saat önce"
                        else -> "⚫ ${timeDiff / 86400000} gün önce"
                    }
                }
                else -> "⚫ Çevrimdışı"
            }
            statusText
        }
        
        holder.statusText.text = statusText
        
        holder.itemView.setOnClickListener {
            onFriendClick(friend)
        }
        
        holder.deleteButton.setOnClickListener {
            onFriendDelete(friend)
        }
    }

    override fun getItemCount(): Int = friends.size
}
