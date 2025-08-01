package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var attachButton: android.widget.ImageButton
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
        setupToolbar()
        
        // Create or get chat
        if (chatId.isEmpty()) {
            createChatWithPartner()
        } else {
            // Load messages from Firebase
            loadMessages()
            
            // Listen for real-time messages
            setupMessageListener()
        }
    }

    private fun createChatWithPartner() {
        lifecycleScope.launch {
            try {
                // Get partner user ID
                val partnerResult = firebaseService.searchUserByUsername(chatPartnerName)
                if (partnerResult.isSuccess) {
                    val partnerUser = partnerResult.getOrNull()
                    if (partnerUser != null) {
                        val chatResult = firebaseService.createChat(currentUserId, partnerUser.id)
                        if (chatResult.isSuccess) {
                            chatId = chatResult.getOrNull() ?: ""
                            Log.d("ChatActivity", "Chat created with ID: $chatId")
                            
                            // Now load messages and setup listener
                            loadMessages()
                            setupMessageListener()
                        } else {
                            Toast.makeText(this@ChatActivity, "Sohbet oluşturulamadı", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ChatActivity, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Kullanıcı arama hatası", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        attachButton = findViewById(R.id.buttonAttach)
        chatTitleText = findViewById(R.id.chatTitleText)
        
        // Set chat title
        chatTitleText.text = chatPartnerName
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
        
        attachButton.setOnClickListener {
            showAttachmentDialog()
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
        if (messageText.isNotEmpty()) {
            if (chatId.isNotEmpty()) {
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
            } else {
                Toast.makeText(this@ChatActivity, "Sohbet henüz hazır değil", Toast.LENGTH_SHORT).show()
            }
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

    private fun showAttachmentDialog() {
        val options = arrayOf("Resim Seç", "Video Seç")
        AlertDialog.Builder(this)
            .setTitle("Dosya Ekle")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectImage()
                    1 -> selectVideo()
                }
            }
            .setNegativeButton("İptal") { _, _ -> }
            .show()
    }

    private fun selectImage() {
        // For now, we'll simulate image selection
        // In a real app, you would use Intent to pick from gallery
        val imageUrl = "https://example.com/sample-image.jpg"
        sendMediaMessage(imageUrl, "image/jpeg", "Resim")
    }

    private fun selectVideo() {
        // For now, we'll simulate video selection
        // In a real app, you would use Intent to pick from gallery
        val videoUrl = "https://example.com/sample-video.mp4"
        sendMediaMessage(videoUrl, "video/mp4", "Video")
    }

    private fun sendMediaMessage(mediaUrl: String, mimeType: String, fileName: String) {
        if (chatId.isNotEmpty()) {
            val chatMessage = ChatMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderUsername = currentUsername,
                content = "[$fileName]",
                messageType = if (mimeType.startsWith("image/")) MessageType.IMAGE else MessageType.VIDEO,
                timestamp = System.currentTimeMillis(),
                mediaUrl = mediaUrl,
                mediaFileName = fileName
            )
            
            lifecycleScope.launch {
                try {
                    val result = firebaseService.sendMessage(chatMessage)
                    if (result.isSuccess) {
                        Toast.makeText(this@ChatActivity, "$fileName gönderildi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ChatActivity, "$fileName gönderilemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Sohbet bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}