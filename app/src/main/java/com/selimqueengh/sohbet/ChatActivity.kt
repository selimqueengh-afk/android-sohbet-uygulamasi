package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.models.ChatMessage
import com.selimqueengh.sohbet.models.MessageType
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitleText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
    private val messageList = mutableListOf<Message>()
    private var chatPartnerName: String = ""
    private var chatId: String = ""
    private var currentUserId: String = ""
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get data from intent
        chatPartnerName = intent.getStringExtra("chat_partner") ?: "Chat"
        chatId = intent.getStringExtra("chat_id") ?: ""
        
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        currentUsername = sharedPreferences.getString("username", "") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Load messages from Firebase
        loadMessages()
        
        // Listen for real-time messages
        if (chatId.isNotEmpty()) {
            setupMessageListener()
        }
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
        messageAdapter = MessageAdapter(messageList)
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

    private fun loadMessages() {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getMessages(chatId)
                    if (result.isSuccess) {
                        val messages = result.getOrNull() ?: emptyList()
                        messageList.clear()
                        
                        // Convert ChatMessage to Message for adapter
                        messages.forEach { chatMessage ->
                            val message = Message(
                                text = chatMessage.content,
                                sender = if (chatMessage.senderId == currentUserId) "Ben" else chatMessage.senderUsername,
                                isSentByUser = chatMessage.senderId == currentUserId,
                                timestamp = chatMessage.timestamp
                            )
                            messageList.add(message)
                        }
                        
                        messageAdapter.notifyDataSetChanged()
                        if (messageList.isNotEmpty()) {
                            recyclerView.scrollToPosition(messageList.size - 1)
                        }
                    } else {
                        Toast.makeText(this@ChatActivity, "Mesajlar yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Fallback to sample messages if no chat ID
            addSampleMessages()
        }
    }

    private fun setupMessageListener() {
        firebaseService.listenToMessages(chatId) { chatMessage ->
            // Convert ChatMessage to Message
            val message = Message(
                text = chatMessage.content,
                sender = if (chatMessage.senderId == currentUserId) "Ben" else chatMessage.senderUsername,
                isSentByUser = chatMessage.senderId == currentUserId,
                timestamp = chatMessage.timestamp
            )
            
            // Add message to list and update UI
            runOnUiThread {
                messageList.add(message)
                messageAdapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty() && chatId.isNotEmpty()) {
            val chatMessage = ChatMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderUsername = currentUsername,
                content = messageText,
                messageType = MessageType.TEXT,
                timestamp = System.currentTimeMillis()
            )
            
            lifecycleScope.launch {
                try {
                    val result = firebaseService.sendMessage(chatMessage)
                    if (result.isSuccess) {
                        messageEditText.text.clear()
                    } else {
                        Toast.makeText(this@ChatActivity, "Mesaj gönderilemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (messageText.isNotEmpty()) {
            // Fallback for demo purposes
            val message = Message(
                text = messageText,
                sender = "Ben",
                isSentByUser = true,
                timestamp = System.currentTimeMillis()
            )
            messageList.add(message)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
            messageEditText.text.clear()
        }
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