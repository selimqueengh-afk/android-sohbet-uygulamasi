package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.content.Intent
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
import com.selimqueengh.sohbet.models.ChatMessage
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
    private var partnerUserId: String = ""
    private var partnerUser: com.selimqueengh.sohbet.models.User? = null
    private var isPartnerTyping: Boolean = false
    
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
        
        // Create or get chat
        if (chatId.isEmpty()) {
            createChatWithPartner()
        } else {
            // Load partner user info and setup status listener
            loadPartnerUserInfo()
            
            // Load messages from Firebase
            loadMessages()
            
            // Listen for real-time messages
            setupRealtimeMessageListener()
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
                        // Store partner user info
                        this@ChatActivity.partnerUser = partnerUser
                        partnerUserId = partnerUser.id
                        
                        // Create Realtime Database chat
                        chatId = firebaseService.createRealtimeChat(currentUserId, partnerUser.id)
                        Log.d("ChatActivity", "Realtime chat created with ID: $chatId")
                        
                        // Setup status listener
                        setupPartnerStatusListener()
                        
                        // Now setup message listener
                        setupRealtimeMessageListener()
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
        
        // Set initial chat title
        chatTitleText.text = chatPartnerName
    }
    
    private fun loadPartnerUserInfo() {
        lifecycleScope.launch {
            try {
                // Get partner user info
                val partnerResult = firebaseService.searchUserByUsername(chatPartnerName)
                if (partnerResult.isSuccess) {
                    partnerUser = partnerResult.getOrNull()
                    if (partnerUser != null) {
                        partnerUserId = partnerUser!!.id
                        // Setup status listener
                        setupPartnerStatusListener()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error loading partner user info", e)
            }
        }
    }
    
    private fun setupPartnerStatusListener() {
        if (partnerUserId.isNotEmpty()) {
            firebaseService.listenToUserStatus(partnerUserId) { user ->
                partnerUser = user
                updateChatTitle()
            }
            
            // Listen to typing indicator
            firebaseService.listenToUserTyping(partnerUserId) { isTyping, typingTo ->
                isPartnerTyping = isTyping && typingTo == chatId
                updateTypingIndicator()
            }
        }
    }
    
    private fun updateTypingIndicator() {
        runOnUiThread {
            val statusView = findViewById<TextView>(R.id.chatStatusText)
            if (isPartnerTyping) {
                // Show typing indicator
                statusView?.text = "Yazıyor..."
                statusView?.visibility = android.view.View.VISIBLE
            } else {
                // Hide typing indicator and show normal status
                updateChatTitle()
            }
        }
    }
    
    private fun updateChatTitle() {
        partnerUser?.let { user ->
            val statusText = if (user.isOnline) {
                "Çevrimiçi"
            } else {
                // Son görülme zamanını hesapla
                when (val lastSeen = user.lastSeen) {
                    is com.google.firebase.Timestamp -> {
                        val timeDiff = System.currentTimeMillis() - lastSeen.toDate().time
                        when {
                            timeDiff < 60000 -> "Az önce" // 1 dakika
                            timeDiff < 3600000 -> "${timeDiff / 60000} dakika önce" // 1 saat
                            timeDiff < 86400000 -> "${timeDiff / 3600000} saat önce" // 1 gün
                            else -> "${timeDiff / 86400000} gün önce"
                        }
                    }
                    is Long -> {
                        val timeDiff = System.currentTimeMillis() - lastSeen
                        when {
                            timeDiff < 60000 -> "Az önce"
                            timeDiff < 3600000 -> "${timeDiff / 60000} dakika önce"
                            timeDiff < 86400000 -> "${timeDiff / 3600000} saat önce"
                            else -> "${timeDiff / 86400000} gün önce"
                        }
                    }
                    else -> "Çevrimdışı"
                }
            }
            
            runOnUiThread {
                chatTitleText.text = user.displayName
                findViewById<TextView>(R.id.chatStatusText)?.text = statusText
                findViewById<TextView>(R.id.chatStatusText)?.visibility = android.view.View.VISIBLE
            }
        }
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
        
        // Add typing indicator
        messageEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
                if (realCurrentUserId.isNotEmpty() && partnerUserId.isNotEmpty()) {
                    lifecycleScope.launch {
                        firebaseService.setUserTyping(realCurrentUserId, s?.isNotEmpty() == true, chatId)
                    }
                }
            }
        })
    }

    private fun loadMessages() {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    Log.d("ChatActivity", "Loading messages for chatId: $chatId")
                    Log.d("ChatActivity", "Current user ID: $currentUserId")
                    Log.d("ChatActivity", "Current username: $currentUsername")
                    
                    // Check if user is authenticated
                    val currentUser = firebaseService.getCurrentUser()
                    if (currentUser == null) {
                        Log.e("ChatActivity", "User not authenticated")
                        Toast.makeText(this@ChatActivity, "Kullanıcı girişi yapılmamış", Toast.LENGTH_SHORT).show()
                        addSampleMessages()
                        return@launch
                    }
                    
                    val result = firebaseService.getMessages(chatId)
                    if (result.isSuccess) {
                        val messages = result.getOrNull() ?: emptyList()
                        Log.d("ChatActivity", "Loaded ${messages.size} messages")
                        messageList.clear()
                        
                        // Convert ChatMessage to Message for adapter
                        messages.forEach { chatMessage ->
                            // Firebase Auth'dan gerçek user ID'yi al
                            val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
                            val isSentByCurrentUser = chatMessage.senderId == realCurrentUserId
                            
                            val message = Message(
                                text = chatMessage.content,
                                sender = if (isSentByCurrentUser) "Ben" else chatMessage.senderUsername,
                                isSentByUser = isSentByCurrentUser,
                                timestamp = chatMessage.timestamp,
                                messageType = chatMessage.messageType,
                                mediaUrl = chatMessage.mediaUrl,
                                mediaType = chatMessage.mediaType
                            )
                            messageList.add(message)
                        }
                        
                        messageAdapter.notifyDataSetChanged()
                        if (messageList.isNotEmpty()) {
                            recyclerView.scrollToPosition(messageList.size - 1)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("ChatActivity", "Failed to load messages: $error")
                        Toast.makeText(this@ChatActivity, "Mesajlar yüklenemedi: ${error?.message}", Toast.LENGTH_SHORT).show()
                        // Fallback to sample messages
                        addSampleMessages()
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Exception loading messages", e)
                    Toast.makeText(this@ChatActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Fallback to sample messages
                    addSampleMessages()
                }
            }
        } else {
            // Fallback to sample messages if no chat ID
            addSampleMessages()
        }
    }

    private fun setupRealtimeMessageListener() {
        if (chatId.isNotEmpty()) {
            // Sadece Firestore'dan mesajları dinle (duplicate olmaması için)
            firebaseService.listenToMessages(chatId) { chatMessage ->
                // Firebase Auth'dan gerçek user ID'yi al
                val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
                val isSentByCurrentUser = chatMessage.senderId == realCurrentUserId
                
                val message = Message(
                    text = chatMessage.content,
                    sender = if (isSentByCurrentUser) "Ben" else chatMessage.senderUsername,
                    isSentByUser = isSentByCurrentUser,
                    timestamp = chatMessage.timestamp,
                    messageType = chatMessage.messageType,
                    mediaUrl = chatMessage.mediaUrl,
                    mediaType = chatMessage.mediaType
                )
                
                // Add message to list and update UI
                runOnUiThread {
                    // Check if message already exists to avoid duplicates
                    val existingMessage = messageList.find { 
                        it.text == message.text && it.timestamp == message.timestamp 
                    }
                    if (existingMessage == null) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            if (chatId.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // Firebase Auth'dan gerçek user ID'yi al
                        val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
                        
                        // Firestore'a mesaj gönder (kalıcı kayıt için)
                        val result = firebaseService.sendMessage(chatId, realCurrentUserId, currentUsername, messageText)
                        if (result.isSuccess) {
                            messageEditText.text.clear()
                            // UI'da hemen gösterme - real-time listener'dan gelecek
                            // Bu şekilde duplicate olmayacak
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
                        // Base64'e çevir ve Firestore'a kaydet
                        convertImageToBase64(uri)
                    }
                }
                PICK_VIDEO_REQUEST -> {
                    selectedUri?.let { uri ->
                        // Base64'e çevir ve Firestore'a kaydet
                        convertVideoToBase64(uri)
                    }
                }
            }
        }
    }
    
    private fun convertImageToBase64(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    sendBase64MediaMessage(base64, "image/jpeg", "📷 Resim")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Resim yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun convertVideoToBase64(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    sendBase64MediaMessage(base64, "video/mp4", "🎥 Video")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Video yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun sendBase64MediaMessage(base64Data: String, mediaType: String, caption: String) {
        if (chatId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
                    val result = firebaseService.sendBase64MediaMessage(chatId, realCurrentUserId, currentUsername, base64Data, mediaType, caption)
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