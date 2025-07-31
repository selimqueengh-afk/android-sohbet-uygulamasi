package com.selimqueengh.sohbet.websocket

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ChatWebSocket(
    private val serverUrl: String,
    private val username: String,
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) : WebSocketClient(URI(serverUrl)) {

    companion object {
        private const val TAG = "ChatWebSocket"
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d(TAG, "WebSocket bağlantısı açıldı")
        onConnectionStatusChanged(true)
        
        // Kullanıcı giriş mesajı gönder
        val loginMessage = """
            {
                "type": "login",
                "username": "$username",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        send(loginMessage)
    }

    override fun onMessage(message: String?) {
        message?.let {
            Log.d(TAG, "Mesaj alındı: $it")
            onMessageReceived(it)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "WebSocket bağlantısı kapandı: $code - $reason")
        onConnectionStatusChanged(false)
    }

    override fun onError(ex: Exception?) {
        Log.e(TAG, "WebSocket hatası", ex)
        onConnectionStatusChanged(false)
    }

    fun sendChatMessage(message: String, recipient: String? = null) {
        val chatMessage = """
            {
                "type": "message",
                "sender": "$username",
                "recipient": "$recipient",
                "content": "$message",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        send(chatMessage)
    }

    fun sendTypingStatus(isTyping: Boolean, recipient: String? = null) {
        val typingMessage = """
            {
                "type": "typing",
                "sender": "$username",
                "recipient": "$recipient",
                "isTyping": $isTyping,
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        send(typingMessage)
    }
}