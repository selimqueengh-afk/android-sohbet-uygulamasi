package com.selimqueengh.sohbet.models

data class ChatItem(
    val chatId: String,
    val username: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isOnline: Boolean
)