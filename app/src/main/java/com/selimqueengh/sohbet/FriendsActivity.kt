package com.selimqueengh.sohbet

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.selimqueengh.sohbet.models.Friend

class FriendsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var addFriendButton: FloatingActionButton
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Add sample friends
        addSampleFriends()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewFriends)
        addFriendButton = findViewById(R.id.addFriendButton)
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(friendsList) { friend ->
            // Open chat with selected friend
            openChatWithFriend(friend)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FriendsActivity)
            adapter = friendsAdapter
        }
    }

    private fun setupClickListeners() {
        addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }
    }

    private fun showAddFriendDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_friend, null)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.editTextUsername)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.editTextPhone)

        AlertDialog.Builder(this)
            .setTitle("Arkadaş Ekle")
            .setView(dialogView)
            .setPositiveButton("Ekle") { _, _ ->
                val username = usernameInput.text.toString()
                val phone = phoneInput.text.toString()
                
                if (username.isNotEmpty()) {
                    addNewFriend(username, phone)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun addNewFriend(username: String, phone: String) {
        val newFriend = Friend(username, "Çevrimiçi", "avatar_default")
        friendsList.add(newFriend)
        friendsAdapter.notifyItemInserted(friendsList.size - 1)
        
        // Yeni sohbet oluştur
        createNewChat(newFriend)
    }

    private fun createNewChat(friend: Friend) {
        val chatItem = ChatItem(
            chatId = System.currentTimeMillis().toString(),
            username = friend.name,
            lastMessage = "Yeni sohbet başlatıldı",
            timestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isOnline = true
        )
        
        // MainActivity'ye yeni sohbet ekle
        val intent = Intent()
        intent.putExtra("new_chat", true)
        intent.putExtra("chat_item", chatItem)
        setResult(RESULT_OK, intent)
    }

    private fun openChatWithFriend(friend: Friend) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chat_partner", friend.name)
        intent.putExtra("chat_id", System.currentTimeMillis().toString())
        startActivity(intent)
    }

    private fun addSampleFriends() {
        val sampleFriends = listOf(
            Friend("Ahmet Yılmaz", "Online", "avatar1"),
            Friend("Ayşe Demir", "Son görülme: 2 saat önce", "avatar2"),
            Friend("Mehmet Kaya", "Online", "avatar3"),
            Friend("Fatma Özkan", "Son görülme: 1 gün önce", "avatar4"),
            Friend("Ali Çelik", "Online", "avatar5")
        )
        
        friendsList.addAll(sampleFriends)
        friendsAdapter.notifyDataSetChanged()
    }
}
