package com.selimqueengh.sohbet

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.selimqueengh.sohbet.models.User

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddFriend: FloatingActionButton
    private lateinit var emptyStateLayout: View
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatItem>()
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Örnek sohbetler ekle
        addSampleChats()
        
        // Boş durum kontrolü
        updateEmptyState()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewChats)
        fabAddFriend = findViewById(R.id.fabAddFriend)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
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
            startActivityForResult(intent, REQUEST_CODE_FRIENDS)
        }
    }
    
    private fun updateEmptyState() {
        if (chatList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }
    
    private fun addSampleChats() {
        // Örnek sohbetler ekle (gerçek uygulamada bu veriler veritabanından gelecek)
        val sampleChats = listOf(
            ChatItem(
                chatId = "1",
                username = "Ahmet Yılmaz",
                lastMessage = "Merhaba! Nasılsın?",
                timestamp = System.currentTimeMillis() - 300000,
                unreadCount = 2,
                isOnline = true
            ),
            ChatItem(
                chatId = "2", 
                username = "Ayşe Demir",
                lastMessage = "Toplantı saat kaçta?",
                timestamp = System.currentTimeMillis() - 600000,
                unreadCount = 0,
                isOnline = false
            ),
            ChatItem(
                chatId = "3",
                username = "Mehmet Kaya", 
                lastMessage = "Dosyayı gönderdim",
                timestamp = System.currentTimeMillis() - 900000,
                unreadCount = 1,
                isOnline = true
            )
        )
        
        chatList.addAll(sampleChats)
        chatAdapter.notifyDataSetChanged()
    }
    
    // Yeni sohbet ekleme metodu
    fun addNewChat(chatItem: ChatItem) {
        chatList.add(0, chatItem) // En üste ekle
        chatAdapter.notifyItemInserted(0)
        updateEmptyState()
    }
    
    // Sohbet silme metodu
    fun removeChat(chatId: String) {
        val index = chatList.indexOfFirst { it.chatId == chatId }
        if (index != -1) {
            chatList.removeAt(index)
            chatAdapter.notifyItemRemoved(index)
            updateEmptyState()
        }
    }
    
    // Son mesajı güncelleme metodu
    fun updateLastMessage(chatId: String, message: String, timestamp: Long) {
        val index = chatList.indexOfFirst { it.chatId == chatId }
        if (index != -1) {
            chatList[index] = chatList[index].copy(
                lastMessage = message,
                timestamp = timestamp,
                unreadCount = chatList[index].unreadCount + 1
            )
            chatAdapter.notifyItemChanged(index)
        }
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
        // Ekran geri döndüğünde boş durumu kontrol et
        updateEmptyState()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_FRIENDS && resultCode == RESULT_OK) {
            data?.let { intent ->
                if (intent.getBooleanExtra("new_chat", false)) {
                    val chatItem = intent.getParcelableExtra<ChatItem>("chat_item")
                    chatItem?.let { addNewChat(it) }
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_CODE_FRIENDS = 1001
    }
}