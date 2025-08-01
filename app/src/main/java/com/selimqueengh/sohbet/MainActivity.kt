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
        // FAB removed - moved to friends page
    }
    
    private fun loadChatsFromFirebase() {
        if (currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    // First get friends
                    val friendsResult = firebaseService.getFriends(currentUserId)
                    if (friendsResult.isSuccess) {
                        val friends = friendsResult.getOrNull() ?: emptyList()
                        
                        // Then get chats for each friend
                        val chats = mutableListOf<ChatItem>()
                        for (friend in friends) {
                            val chatResult = firebaseService.getChats(currentUserId)
                            if (chatResult.isSuccess) {
                                val userChats = chatResult.getOrNull() ?: emptyList()
                                val friendChat = userChats.find { chatData ->
                                    val otherUserId = if (chatData["user1Id"] == currentUserId) {
                                        chatData["user2Id"] as? String ?: ""
                                    } else {
                                        chatData["user1Id"] as? String ?: ""
                                    }
                                    otherUserId == friend.id
                                }
                                
                                if (friendChat != null) {
                                    val otherUserId = if (friendChat["user1Id"] == currentUserId) {
                                        friendChat["user2Id"] as? String ?: ""
                                    } else {
                                        friendChat["user1Id"] as? String ?: ""
                                    }
                                    
                                    val userResult = firebaseService.getUsers()
                                    if (userResult.isSuccess) {
                                        val users = userResult.getOrNull() ?: emptyList()
                                        val otherUser = users.find { it.id == otherUserId }
                                        
                                        val chatItem = ChatItem(
                                            chatId = friendChat["chatId"] as? String ?: "",
                                            username = otherUser?.username ?: friend.name,
                                            lastMessage = friendChat["lastMessageContent"] as? String ?: "Henüz mesaj yok",
                                            timestamp = (friendChat["lastMessageTimestamp"] as? java.util.Date)?.time ?: System.currentTimeMillis(),
                                            unreadCount = 0,
                                            isOnline = otherUser?.isOnline ?: false
                                        )
                                        chats.add(chatItem)
                                    }
                                }
                            }
                        }
                        
                        chatList.clear()
                        chatList.addAll(chats)
                        chatAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@MainActivity, "Arkadaşlar yüklenemedi", Toast.LENGTH_SHORT).show()
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