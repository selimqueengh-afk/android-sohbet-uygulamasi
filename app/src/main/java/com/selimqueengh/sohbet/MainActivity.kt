package com.selimqueengh.sohbet

import android.content.Intent
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
    private val chatList = mutableListOf<ChatItem>()
    private var currentUsername: String = ""
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Load chats from Firebase
        loadChats()
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
            intent.putExtra("receiver_id", chatItem.receiverId)
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
    
    private fun loadChats() {
        lifecycleScope.launch {
            try {
                val result = firebaseService.getUserChats()
                if (result.isSuccess) {
                    val chatIds = result.getOrNull() ?: emptyList()
                    // For now, we'll show sample chats until we implement full chat loading
                    addSampleChats()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Sohbetler yüklenemedi", Toast.LENGTH_SHORT).show()
                        addSampleChats()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    addSampleChats()
                }
            }
        }
    }
    
    private fun addSampleChats() {
        val sampleChats = listOf(
            ChatItem(
                chatId = "demo-1",
                username = "Ahmet Yılmaz",
                lastMessage = "Merhaba! Nasılsın?",
                timestamp = System.currentTimeMillis() - 300000,
                unreadCount = 2,
                isOnline = true,
                receiverId = "demo-user-1"
            ),
            ChatItem(
                chatId = "demo-2", 
                username = "Ayşe Demir",
                lastMessage = "Toplantı saat kaçta?",
                timestamp = System.currentTimeMillis() - 600000,
                unreadCount = 0,
                isOnline = false,
                receiverId = "demo-user-2"
            ),
            ChatItem(
                chatId = "demo-3",
                username = "Mehmet Kaya", 
                lastMessage = "Dosyayı gönderdim",
                timestamp = System.currentTimeMillis() - 900000,
                unreadCount = 1,
                isOnline = true,
                receiverId = "demo-user-3"
            )
        )
        
        chatList.clear()
        chatList.addAll(sampleChats)
        chatAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                // Profil ekranına git
                true
            }
            R.id.action_settings -> {
                // Ayarlar ekranına git
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            firebaseService.updateUserStatus(true)
        }
    }
    
    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            firebaseService.updateUserStatus(false)
        }
    }
}