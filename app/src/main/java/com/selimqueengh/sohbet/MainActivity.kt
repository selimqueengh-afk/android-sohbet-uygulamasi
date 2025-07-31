package com.selimqueengh.sohbet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.chat.ChatManager
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), ChatManager.ChatListener {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var statusText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private val chatManager = ChatManager.getInstance(this)
    private var currentUsername: String = ""
    private var isTyping = false
    private var typingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupChatManager()
        
        // Örnek mesajlar ekle
        addSampleMessages()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        statusText = findViewById(R.id.statusText)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        // Typing indicator
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handleTyping(s?.toString()?.isNotEmpty() == true)
            }
        })
    }
    
    private fun setupChatManager() {
        chatManager.addChatListener(this)
        chatManager.connect(currentUsername)
    }
    
    private fun handleTyping(isTyping: Boolean) {
        if (this.isTyping != isTyping) {
            this.isTyping = isTyping
            chatManager.sendTypingStatus(isTyping)
        }
        
        // Typing timeout
        typingJob?.cancel()
        if (isTyping) {
            typingJob = CoroutineScope(Dispatchers.Main).launch {
                delay(3000) // 3 saniye sonra typing durumunu kapat
                this@MainActivity.isTyping = false
                chatManager.sendTypingStatus(false)
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            chatManager.sendMessage(messageText)
            messageEditText.text.clear()
        }
    }

    private fun addSampleMessages() {
        val sampleMessages = listOf(
            Message(
                id = "1",
                text = "Merhaba! Nasılsın?",
                sender = "Ahmet",
                isSentByUser = false,
                timestamp = System.currentTimeMillis() - 300000
            ),
            Message(
                id = "2",
                text = "İyiyim, teşekkürler! Sen nasılsın?",
                sender = currentUsername,
                isSentByUser = true,
                timestamp = System.currentTimeMillis() - 240000
            ),
            Message(
                id = "3",
                text = "Ben de iyiyim. Bu uygulamayı test ediyoruz.",
                sender = "Ayşe",
                isSentByUser = false,
                timestamp = System.currentTimeMillis() - 180000
            ),
            Message(
                id = "4",
                text = "Harika! Çok güzel çalışıyor.",
                sender = currentUsername,
                isSentByUser = true,
                timestamp = System.currentTimeMillis() - 120000
            )
        )
        
        messageList.addAll(sampleMessages)
        messageAdapter.notifyDataSetChanged()
    }
    
    // ChatManager.ChatListener implementations
    override fun onMessageReceived(message: Message) {
        runOnUiThread {
            messageList.add(message)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.scrollToPosition(messageList.size - 1)
        }
    }
    
    override fun onConnectionStatusChanged(isConnected: Boolean) {
        runOnUiThread {
            statusText.text = if (isConnected) "Çevrimiçi" else "Bağlantı kesildi"
            statusText.setTextColor(
                if (isConnected) getColor(R.color.online_color) 
                else getColor(R.color.offline_color)
            )
        }
    }
    
    override fun onTypingStatusChanged(username: String, isTyping: Boolean, recipient: String?) {
        runOnUiThread {
            if (isTyping) {
                statusText.text = "$username yazıyor..."
            } else {
                statusText.text = "Çevrimiçi"
            }
        }
    }
    
    override fun onUserListUpdated(users: List<com.selimqueengh.sohbet.models.User>) {
        // Kullanıcı listesi güncellendi
    }
    
    override fun onUserStatusChanged() {
        // Kullanıcı durumu değişti
    }
    
    override fun onDestroy() {
        super.onDestroy()
        chatManager.removeChatListener(this)
        chatManager.disconnect()
        typingJob?.cancel()
    }
}