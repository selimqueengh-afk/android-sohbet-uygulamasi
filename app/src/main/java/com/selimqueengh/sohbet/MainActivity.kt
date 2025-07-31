package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddFriend: FloatingActionButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
    private val chatList = mutableListOf<ChatItem>()
    private var currentUserId: String = ""
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Load chats from Firebase
        loadChatsFromFirebase()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewChats)
        fabAddFriend = findViewById(R.id.fabAddFriend)
        
        // Toolbar'ı ayarla
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList) { chatItem ->
            // Sohbete git
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chat_partner", chatItem.username)
            intent.putExtra("chat_id", chatItem.chatId)
            startActivity(intent)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        fabAddFriend.setOnClickListener {
            // Arkadaş ekleme ekranına git
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadChatsFromFirebase() {
        if (currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getChats(currentUserId)
                    if (result.isSuccess) {
                        val chats = result.getOrNull() ?: emptyList()
                        chatList.clear()
                        
                        // Convert Firebase chats to ChatItem
                        chats.forEach { chatData ->
                            val otherUserId = if (chatData["user1Id"] == currentUserId) {
                                chatData["user2Id"] as? String ?: ""
                            } else {
                                chatData["user1Id"] as? String ?: ""
                            }
                            
                            if (otherUserId.isNotEmpty()) {
                                // Get user info for display
                                val userResult = firebaseService.getUsers()
                                if (userResult.isSuccess) {
                                    val users = userResult.getOrNull() ?: emptyList()
                                    val otherUser = users.find { it.id == otherUserId }
                                    
                                    val chatItem = ChatItem(
                                        chatId = chatData["chatId"] as? String ?: "",
                                        username = otherUser?.username ?: "Bilinmeyen Kullanıcı",
                                        lastMessage = chatData["lastMessageContent"] as? String ?: "Henüz mesaj yok",
                                        timestamp = (chatData["lastMessageTimestamp"] as? java.util.Date)?.time ?: System.currentTimeMillis(),
                                        unreadCount = 0, // TODO: Implement unread count
                                        isOnline = otherUser?.isOnline ?: false
                                    )
                                    chatList.add(chatItem)
                                }
                            }
                        }
                        
                        chatAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@MainActivity, "Sohbetler yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Menu removed as requested
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh chats when returning to this activity
        loadChatsFromFirebase()
    }
}