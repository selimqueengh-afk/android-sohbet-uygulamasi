package com.selimqueengh.sohbet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val id: String = "",
    val text: String,
    val sender: String,
    val recipient: String? = null,
    val isSentByUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false
) : Parcelable

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}