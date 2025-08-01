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
import com.selimqueengh.sohbet.models.ChatItem
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
    private val chatList = mutableListOf<com.selimqueengh.sohbet.models.ChatItem>()
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
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_chats -> {
                    // Already on chats page
                    true
                }
                R.id.nav_friends -> {
                    val intent = Intent(this, FriendsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    // TODO: Add profile activity
                    Toast.makeText(this, "Profil yakında gelecek", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadChatsFromFirebase() {
        if (currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    // Get all chats for current user
                    val chatResult = firebaseService.getChats(currentUserId)
                    if (chatResult.isSuccess) {
                        val userChats = chatResult.getOrNull() ?: emptyList()
                        val chats = mutableListOf<ChatItem>()
                        
                        for (chatData in userChats) {
                            val participants = chatData["participants"] as? List<String> ?: emptyList()
                            val otherUserId = participants.find { it != currentUserId } ?: continue
                            
                            // Get other user info
                            val otherUserResult = firebaseService.getUserById(otherUserId)
                            val otherUser = otherUserResult.getOrNull()
                            
                            val chatItem = ChatItem(
                                chatId = chatData["chatId"] as? String ?: "",
                                username = otherUser?.username ?: "Bilinmeyen Kullanıcı",
                                lastMessage = chatData["lastMessage"] as? String ?: "Henüz mesaj yok",
                                timestamp = (chatData["lastMessageTimestamp"] as? java.util.Date)?.time ?: System.currentTimeMillis(),
                                unreadCount = 0,
                                isOnline = otherUser?.isOnline ?: false
                            )
                            chats.add(chatItem)
                        }
                        
                        chatList.clear()
                        chatList.addAll(chats)
                        chatAdapter.notifyDataSetChanged()
                        
                        // Show empty state if no chats
                        val emptyStateLayout = findViewById<android.view.View>(R.id.emptyStateLayout)
                        if (chats.isEmpty()) {
                            emptyStateLayout?.visibility = android.view.View.VISIBLE
                        } else {
                            emptyStateLayout?.visibility = android.view.View.GONE
                        }
                        
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