package com.selimqueengh.sohbet.models

import java.util.Date

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val content: String = "",
    val messageType: String = "text",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false
)