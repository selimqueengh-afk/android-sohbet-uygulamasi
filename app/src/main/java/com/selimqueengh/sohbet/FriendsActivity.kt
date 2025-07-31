package com.selimqueengh.sohbet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.selimqueengh.sohbet.models.Friend
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var addFriendButton: FloatingActionButton
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        
        // Load users from Firebase
        loadUsers()
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
            // TODO: Implement add friend functionality
            // For now, just add a sample friend
            addSampleFriend()
        }
    }

    private fun openChatWithFriend(friend: Friend) {
        lifecycleScope.launch {
            try {
                val currentUserId = firebaseService.getCurrentUser()?.uid
                if (currentUserId != null) {
                    val result = firebaseService.createChat(currentUserId, friend.userId)
                    if (result.isSuccess) {
                        val chatId = result.getOrNull() ?: ""
                        val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                        intent.putExtra("chat_partner", friend.name)
                        intent.putExtra("chat_id", chatId)
                        intent.putExtra("receiver_id", friend.userId)
                        startActivity(intent)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@FriendsActivity, "Sohbet oluşturulamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@FriendsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val result = firebaseService.getUsers()
                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    val currentUserId = firebaseService.getCurrentUser()?.uid
                    
                    val friends = users.filter { it.id != currentUserId }.map { user ->
                        Friend(
                            name = user.displayName,
                            status = if (user.isOnline) "Online" else "Offline",
                            avatar = user.avatarUrl ?: "avatar_default",
                            userId = user.id
                        )
                    }
                    
                    runOnUiThread {
                        friendsList.clear()
                        friendsList.addAll(friends)
                        friendsAdapter.notifyDataSetChanged()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@FriendsActivity, "Kullanıcılar yüklenemedi", Toast.LENGTH_SHORT).show()
                        addSampleFriends()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@FriendsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    addSampleFriends()
                }
            }
        }
    }
    
    private fun addSampleFriends() {
        val sampleFriends = listOf(
            Friend("Ahmet Yılmaz", "Online", "avatar1", "demo-user-1"),
            Friend("Ayşe Demir", "Son görülme: 2 saat önce", "avatar2", "demo-user-2"),
            Friend("Mehmet Kaya", "Online", "avatar3", "demo-user-3"),
            Friend("Fatma Özkan", "Son görülme: 1 gün önce", "avatar4", "demo-user-4"),
            Friend("Ali Çelik", "Online", "avatar5", "demo-user-5")
        )
        
        friendsList.clear()
        friendsList.addAll(sampleFriends)
        friendsAdapter.notifyDataSetChanged()
    }

    private fun addSampleFriend() {
        val newFriend = Friend("Yeni Arkadaş", "Online", "avatar_default")
        friendsList.add(newFriend)
        friendsAdapter.notifyItemInserted(friendsList.size - 1)
    }
}
