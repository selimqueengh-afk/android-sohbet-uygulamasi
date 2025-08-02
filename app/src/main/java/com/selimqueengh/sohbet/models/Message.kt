package com.selimqueengh.sohbet.models

data class Message(
    val text: String,
    val sender: String,
    val isSentByUser: Boolean,
    val timestamp: Long,
    val messageType: String = "text"
)