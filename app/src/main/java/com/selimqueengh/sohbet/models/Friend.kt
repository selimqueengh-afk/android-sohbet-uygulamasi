package com.selimqueengh.sohbet.models

import com.google.firebase.Timestamp

data class Friend(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val status: String = "offline",
    val avatar: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Any? = null
)
