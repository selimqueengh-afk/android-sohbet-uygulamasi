package com.selimqueengh.sohbet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import com.selimqueengh.sohbet.models.ChatMessage
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitleText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private var chatPartnerName: String = ""
    private var chatId: String = ""
    private var receiverId: String = ""
    private lateinit var firebaseService: FirebaseService
    private var messageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get chat partner name and chat ID from intent
        chatPartnerName = intent.getStringExtra("chat_partner") ?: "Chat"
        chatId = intent.getStringExtra("chat_id") ?: ""
        receiverId = intent.getStringExtra("receiver_id") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Load messages from Firebase
        loadMessages()
        setupMessageListener()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        chatTitleText = findViewById(R.id.chatTitleText)
        
        // Set chat title
        chatTitleText.text = chatPartnerName
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList.map { it.toMessage() })
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty() && chatId.isNotEmpty()) {
            sendButton.isEnabled = false
            
            lifecycleScope.launch {
                try {
                    val result = firebaseService.sendMessage(chatId, messageText, receiverId)
                    if (result.isSuccess) {
                        runOnUiThread {
                            messageEditText.text.clear()
                            sendButton.isEnabled = true
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Mesaj gönderilemedi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            sendButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        sendButton.isEnabled = true
                    }
                }
            }
        }
    }
    
    private fun loadMessages() {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getChatMessages(chatId)
                    if (result.isSuccess) {
                        messageList.clear()
                        messageList.addAll(result.getOrNull() ?: emptyList())
                        runOnUiThread {
                            messageAdapter.updateMessages(messageList.map { it.toMessage() })
                            if (messageList.isNotEmpty()) {
                                recyclerView.scrollToPosition(messageList.size - 1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "Mesajlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun setupMessageListener() {
        if (chatId.isNotEmpty()) {
            messageListener = firebaseService.listenToMessages(chatId) { message ->
                runOnUiThread {
                    if (!messageList.any { it.id == message.id }) {
                        messageList.add(message)
                        messageAdapter.updateMessages(messageList.map { it.toMessage() })
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
    
    private fun ChatMessage.toMessage(): Message {
        val currentUserId = firebaseService.getCurrentUser()?.uid ?: ""
        return Message(
            text = this.content,
            sender = this.senderUsername,
            isSentByUser = this.senderId == currentUserId,
            timestamp = this.timestamp
        )
    }

    private fun addSampleMessages() {
        val sampleMessages = listOf(
            Message(
                text = "Merhaba! Nasılsın?",
                sender = chatPartnerName,
                isSentByUser = false,
                timestamp = System.currentTimeMillis() - 300000
            ),
            Message(
                text = "İyiyim, teşekkürler! Sen nasılsın?",
                sender = "Ben",
                isSentByUser = true,
                timestamp = System.currentTimeMillis() - 240000
            ),
            Message(
                text = "Ben de iyiyim. Bu sohbeti test ediyoruz.",
                sender = chatPartnerName,
                isSentByUser = false,
                timestamp = System.currentTimeMillis() - 180000
            ),
            Message(
                text = "Harika! Çok güzel çalışıyor.",
                sender = "Ben",
                isSentByUser = true,
                timestamp = System.currentTimeMillis() - 120000
            )
        )
        
        messageList.addAll(sampleMessages)
        messageAdapter.notifyDataSetChanged()
    }
}