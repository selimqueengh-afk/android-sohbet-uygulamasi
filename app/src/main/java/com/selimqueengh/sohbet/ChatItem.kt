package com.selimqueengh.sohbet

data class ChatItem(
    val chatId: String,
    val username: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val profileImage: String? = null,
    val receiverId: String = ""
)