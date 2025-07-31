package com.selimqueengh.sohbet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val content: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val replyToMessageId: String? = null,
    val mediaUrl: String? = null,
    val mediaFileName: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
) : Parcelable {
    
    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getFormattedDate(): String {
        val date = java.util.Date(timestamp)
        val today = java.util.Calendar.getInstance()
        val messageDate = java.util.Calendar.getInstance().apply { time = date }
        
        return when {
            today.get(java.util.Calendar.DAY_OF_YEAR) == messageDate.get(java.util.Calendar.DAY_OF_YEAR) -> {
                "Bugün"
            }
            today.get(java.util.Calendar.DAY_OF_YEAR) - 1 == messageDate.get(java.util.Calendar.DAY_OF_YEAR) -> {
                "Dün"
            }
            else -> {
                val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                formatter.format(date)
            }
        }
    }
    
    fun isFromCurrentUser(currentUserId: String): Boolean {
        return senderId == currentUserId
    }
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    STICKER
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}