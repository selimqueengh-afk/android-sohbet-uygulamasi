package com.selimqueengh.sohbet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: @RawValue Any? = null,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val typingTo: String? = null
) : Parcelable

enum class UserStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY
}