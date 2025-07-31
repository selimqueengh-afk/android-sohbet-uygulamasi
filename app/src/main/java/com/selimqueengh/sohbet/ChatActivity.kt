package com.selimqueengh.sohbet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTitleText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private var chatPartnerName: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUserId: String = ""
    private var currentUsername: String = ""
    private var chatId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Firebase başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Get chat partner name from intent
        chatPartnerName = intent.getStringExtra("chat_partner") ?: "Chat"
        currentUsername = intent.getStringExtra("current_username") ?: "User"
        
        // Kullanıcı kontrolü
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            setupChat()
        } else {
            Toast.makeText(this, "Kullanıcı girişi yapılmamış", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupChat() {
        // Chat ID oluştur (kullanıcılar arası benzersiz)
        chatId = createChatId(currentUserId, chatPartnerName)
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }
    
    private fun createChatId(userId: String, partnerName: String): String {
        // Basit bir chat ID oluştur
        return "${userId}_${partnerName.hashCode()}"
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
                sender = currentUsername,
                senderUid = currentUserId,
                recipient = chatPartnerName,
                isSentByUser = true,
                timestamp = System.currentTimeMillis()
            )
            
            // Firestore'a mesajı kaydet
            saveMessageToFirestore(message)
            
            messageEditText.text.clear()
        }
    }
    
    private fun saveMessageToFirestore(message: Message) {
        val messageData = hashMapOf(
            "text" to message.text,
            "sender" to message.sender,
            "senderUid" to message.senderUid,
            "recipient" to message.recipient,
            "timestamp" to message.timestamp,
            "messageType" to message.messageType.name,
            "isRead" to message.isRead,
            "isDelivered" to message.isDelivered
        )
        
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener { documentReference ->
                // Mesaj başarıyla kaydedildi
                message.id = documentReference.id
                messageList.add(message)
                messageAdapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Mesaj gönderilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadMessages() {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Mesajlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                messageList.clear()
                for (document in snapshot!!) {
                    val message = Message(
                        id = document.id,
                        text = document.getString("text") ?: "",
                        sender = document.getString("sender") ?: "",
                        senderUid = document.getString("senderUid") ?: "",
                        recipient = document.getString("recipient"),
                        timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                        messageType = MessageType.valueOf(document.getString("messageType") ?: "TEXT"),
                        isRead = document.getBoolean("isRead") ?: false,
                        isDelivered = document.getBoolean("isDelivered") ?: false
                    )
                    messageList.add(message)
                }
                messageAdapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    recyclerView.scrollToPosition(messageList.size - 1)
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
}