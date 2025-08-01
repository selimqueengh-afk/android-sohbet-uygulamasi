package com.selimqueengh.sohbet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatItem(
    val chatId: String,
    val username: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val profileImage: String? = null
) : Parcelable