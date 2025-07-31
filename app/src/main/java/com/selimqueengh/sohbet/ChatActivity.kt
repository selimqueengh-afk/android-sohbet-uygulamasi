package com.selimqueengh.sohbet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitleText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private var chatPartnerName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get chat partner name from intent
        chatPartnerName = intent.getStringExtra("chat_partner") ?: "Chat"
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Add sample messages for this chat
        addSampleMessages()
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

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val message = Message(
                text = messageText,
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
            Message("Merhaba! Nasılsın?", false, System.currentTimeMillis() - 300000),
            Message("İyiyim, teşekkürler! Sen nasılsın?", true, System.currentTimeMillis() - 240000),
            Message("Ben de iyiyim. Bu sohbeti test ediyoruz.", false, System.currentTimeMillis() - 180000),
            Message("Harika! Çok güzel çalışıyor.", true, System.currentTimeMillis() - 120000)
        )
        
        messageList.addAll(sampleMessages)
        messageAdapter.notifyDataSetChanged()
    }
}