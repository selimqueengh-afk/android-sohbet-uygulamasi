package com.selimqueengh.sohbet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.Timestamp

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: Any? = null,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val typingTo: String? = null
) : Parcelable

enum class UserStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    TYPING
}