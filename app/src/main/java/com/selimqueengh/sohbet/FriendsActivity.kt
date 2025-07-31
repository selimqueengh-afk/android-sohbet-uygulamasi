package com.selimqueengh.sohbet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
            // TODO: Implement add friend functionality
            // For now, just add a sample friend
            addSampleFriend()
        }
    }

    private fun openChatWithFriend(friend: Friend) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chat_partner", friend.name)
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

    private fun addSampleFriend() {
        val newFriend = Friend("Yeni Arkadaş", "Online", "avatar_default")
        friendsList.add(newFriend)
        friendsAdapter.notifyItemInserted(friendsList.size - 1)
    }
}
