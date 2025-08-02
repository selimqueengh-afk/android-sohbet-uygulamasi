package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import com.selimqueengh.sohbet.models.Message
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
    
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_VIDEO_REQUEST = 2
    }

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
        
        // Load messages and setup listener
        loadMessages()
        setupMessageListener()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        attachButton = findViewById(R.id.buttonAttach)
        chatTitleText = findViewById(R.id.chatTitleText)
        
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
        
        attachButton.setOnClickListener {
            showAttachmentDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun loadMessages() {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getMessages(chatId)
                    if (result.isSuccess) {
                        val messages = result.getOrNull() ?: emptyList()
                        messageList.clear()
                        
                        messages.forEach { chatMessage ->
                            val isSentByCurrentUser = chatMessage.senderId == currentUserId
                            val message = Message(
                                text = chatMessage.content,
                                sender = if (isSentByCurrentUser) "Ben" else chatMessage.senderUsername,
                                isSentByUser = isSentByCurrentUser,
                                timestamp = chatMessage.timestamp,
                                messageType = chatMessage.messageType
                            )
                            messageList.add(message)
                        }
                        
                        messageAdapter.notifyDataSetChanged()
                        if (messageList.isNotEmpty()) {
                            recyclerView.scrollToPosition(messageList.size - 1)
                        }
                    } else {
                        addSampleMessages()
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error loading messages", e)
                    addSampleMessages()
                }
            }
        } else {
            addSampleMessages()
        }
    }

    private fun setupMessageListener() {
        if (chatId.isNotEmpty()) {
            firebaseService.listenToMessages(chatId) { chatMessage ->
                val isSentByCurrentUser = chatMessage.senderId == currentUserId
                val message = Message(
                    text = chatMessage.content,
                    sender = if (isSentByCurrentUser) "Ben" else chatMessage.senderUsername,
                    isSentByUser = isSentByCurrentUser,
                    timestamp = chatMessage.timestamp,
                    messageType = chatMessage.messageType
                )
                
                runOnUiThread {
                    messageList.add(message)
                    messageAdapter.notifyItemInserted(messageList.size - 1)
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            if (chatId.isNotEmpty()) {
                // Clear input immediately
                messageEditText.text.clear()
                
                // Send to Firebase
                lifecycleScope.launch {
                    try {
                        val result = firebaseService.sendMessage(chatId, currentUserId, currentUsername, messageText)
                        if (!result.isSuccess) {
                            runOnUiThread {
                                Toast.makeText(this@ChatActivity, "Mesaj gönderilemedi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
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
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && data != null) {
            val selectedUri: Uri? = data.data
            
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedUri?.let { uri ->
                        sendMediaMessage(uri.toString(), "image/jpeg", "📷 Resim")
                    }
                }
                PICK_VIDEO_REQUEST -> {
                    selectedUri?.let { uri ->
                        sendMediaMessage(uri.toString(), "video/mp4", "🎥 Video")
                    }
                }
            }
        }
    }
    
    private fun sendMediaMessage(mediaUrl: String, mediaType: String, caption: String) {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.sendMediaMessage(chatId, currentUserId, currentUsername, mediaUrl, mediaType)
                    if (result.isSuccess) {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Medya gönderildi", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ChatActivity, "Medya gönderilemedi", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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