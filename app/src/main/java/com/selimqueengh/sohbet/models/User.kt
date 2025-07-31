package com.selimqueengh.sohbet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val status: String = "",
    val isTyping: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    fun getStatusText(): String {
        return when {
            isTyping -> "yazıyor..."
            isOnline -> "Çevrimiçi"
            lastSeen > 0 -> {
                val diff = System.currentTimeMillis() - lastSeen
                when {
                    diff < 60000 -> "Az önce görüldü"
                    diff < 3600000 -> "${diff / 60000} dakika önce görüldü"
                    diff < 86400000 -> "${diff / 3600000} saat önce görüldü"
                    else -> "${diff / 86400000} gün önce görüldü"
                }
            }
            else -> "Çevrimdışı"
        }
    }
    
    fun getDisplayNameOrUsername(): String {
        return displayName.ifEmpty { username }
    }
}