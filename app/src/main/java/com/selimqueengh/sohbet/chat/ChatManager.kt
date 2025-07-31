package com.selimqueengh.sohbet.chat

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.selimqueengh.sohbet.Message
import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.websocket.ChatWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ChatManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ChatManager"
        private const val SERVER_URL = "wss://echo.websocket.org" // Test için echo server
        
        @Volatile
        private var INSTANCE: ChatManager? = null
        
        fun getInstance(context: Context): ChatManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatManager(context).also { INSTANCE = it }
            }
        }
    }
    
    private var webSocket: ChatWebSocket? = null
    private val gson = Gson()
    private val currentUser: User? = null
    private val onlineUsers = mutableListOf<User>()
    private val chatListeners = mutableListOf<ChatListener>()
    
    fun connect(username: String) {
        try {
            webSocket = ChatWebSocket(
                serverUrl = SERVER_URL,
                username = username,
                onMessageReceived = { message ->
                    handleIncomingMessage(message)
                },
                onConnectionStatusChanged = { isConnected ->
                    notifyConnectionStatusChanged(isConnected)
                }
            )
            webSocket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket bağlantı hatası", e)
        }
    }
    
    fun disconnect() {
        webSocket?.close()
        webSocket = null
    }
    
    fun sendMessage(message: String, recipient: String? = null) {
        webSocket?.sendChatMessage(message, recipient)
        
        // Yerel mesaj oluştur
        val localMessage = Message(
            id = UUID.randomUUID().toString(),
            text = message,
            sender = currentUser?.username ?: "",
            recipient = recipient,
            isSentByUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        notifyMessageReceived(localMessage)
    }
    
    fun sendTypingStatus(isTyping: Boolean, recipient: String? = null) {
        webSocket?.sendTypingStatus(isTyping, recipient)
    }
    
    private fun handleIncomingMessage(messageJson: String) {
        try {
            val messageData = gson.fromJson(messageJson, Map::class.java)
            val messageType = messageData["type"] as? String
            
            when (messageType) {
                "message" -> handleChatMessage(messageData)
                "typing" -> handleTypingStatus(messageData)
                "user_list" -> handleUserList(messageData)
                "user_status" -> handleUserStatus(messageData)
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parse hatası", e)
        } catch (e: Exception) {
            Log.e(TAG, "Mesaj işleme hatası", e)
        }
    }
    
    private fun handleChatMessage(messageData: Map<*, *>) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            text = messageData["content"] as? String ?: "",
            sender = messageData["sender"] as? String ?: "",
            recipient = messageData["recipient"] as? String,
            isSentByUser = false,
            timestamp = (messageData["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
        
        notifyMessageReceived(message)
    }
    
    private fun handleTypingStatus(messageData: Map<*, *>) {
        val username = messageData["sender"] as? String ?: return
        val isTyping = messageData["isTyping"] as? Boolean ?: false
        val recipient = messageData["recipient"] as? String
        
        notifyTypingStatusChanged(username, isTyping, recipient)
    }
    
    private fun handleUserList(messageData: Map<*, *>) {
        // Kullanıcı listesi güncelleme
        notifyUserListUpdated()
    }
    
    private fun handleUserStatus(messageData: Map<*, *>) {
        // Kullanıcı durumu güncelleme
        notifyUserStatusChanged()
    }
    
    // Listener yönetimi
    fun addChatListener(listener: ChatListener) {
        if (!chatListeners.contains(listener)) {
            chatListeners.add(listener)
        }
    }
    
    fun removeChatListener(listener: ChatListener) {
        chatListeners.remove(listener)
    }
    
    private fun notifyMessageReceived(message: Message) {
        chatListeners.forEach { it.onMessageReceived(message) }
    }
    
    private fun notifyConnectionStatusChanged(isConnected: Boolean) {
        chatListeners.forEach { it.onConnectionStatusChanged(isConnected) }
    }
    
    private fun notifyTypingStatusChanged(username: String, isTyping: Boolean, recipient: String?) {
        chatListeners.forEach { it.onTypingStatusChanged(username, isTyping, recipient) }
    }
    
    private fun notifyUserListUpdated() {
        chatListeners.forEach { it.onUserListUpdated(onlineUsers) }
    }
    
    private fun notifyUserStatusChanged() {
        chatListeners.forEach { it.onUserStatusChanged() }
    }
    
    interface ChatListener {
        fun onMessageReceived(message: Message)
        fun onConnectionStatusChanged(isConnected: Boolean)
        fun onTypingStatusChanged(username: String, isTyping: Boolean, recipient: String?)
        fun onUserListUpdated(users: List<User>)
        fun onUserStatusChanged()
    }
}