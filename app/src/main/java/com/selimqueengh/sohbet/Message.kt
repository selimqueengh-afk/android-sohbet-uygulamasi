package com.selimqueengh.sohbet

data class Message(
    val text: String,
    val isSentByUser: Boolean,
    val timestamp: Long
)