package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
    private val friendsList = mutableListOf<Friend>()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupToolbar()
        
        // Load friends from Firebase
        loadFriendsFromFirebase()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewFriends)
        addFriendButton = findViewById(R.id.addFriendButton)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.friendsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Add menu for friend requests
        toolbar.inflateMenu(R.menu.friends_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_friend_requests -> {
                    val intent = Intent(this, FriendRequestsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_friend, null)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.usernameEditText)
        val searchButton = dialogView.findViewById<Button>(R.id.searchButton)
        val sendRequestButton = dialogView.findViewById<Button>(R.id.sendRequestButton)
        
        var foundUser: com.selimqueengh.sohbet.models.User? = null
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Arkadaş Ekle")
            .setView(dialogView)
            .setNegativeButton("İptal") { _, _ -> }
            .create()
        
        searchButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            if (username.isNotEmpty()) {
                searchButton.text = "Aranıyor..."
                searchButton.isEnabled = false
                searchUser(username) { user ->
                    foundUser = user
                    if (user != null) {
                        searchButton.text = "✓ Kullanıcı Bulundu"
                        searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                        sendRequestButton.visibility = android.view.View.VISIBLE
                    } else {
                        searchButton.text = "✗ Kullanıcı Bulunamadı"
                        searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                        sendRequestButton.visibility = android.view.View.GONE
                    }
                    searchButton.isEnabled = true
                }
            } else {
                Toast.makeText(this, "Kullanıcı adı girin", Toast.LENGTH_SHORT).show()
            }
        }
        
        sendRequestButton.setOnClickListener {
            foundUser?.let { user ->
                sendFriendRequest(user.id) {
                    dialog.dismiss()
                    Toast.makeText(this@FriendsActivity, "Arkadaşlık isteği gönderildi", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        dialog.show()
    }

    private fun searchUser(username: String, callback: (com.selimqueengh.sohbet.models.User?) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = firebaseService.searchUserByUsername(username)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    
                    runOnUiThread {
                        if (user != null && user.id != currentUserId) {
                            Log.d("FriendsActivity", "Found user: ${user.username}")
                            callback(user)
                        } else {
                            Log.d("FriendsActivity", "User not found or is current user")
                            callback(null)
                        }
                    }
                } else {
                    Log.e("FriendsActivity", "Failed to search user: ${result.exceptionOrNull()?.message}")
                    runOnUiThread {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendsActivity", "Error searching user", e)
                runOnUiThread {
                    callback(null)
                }
            }
        }
    }

    private fun sendFriendRequest(friendId: String, callback: () -> Unit) {
        lifecycleScope.launch {
            try {
                Log.d("FriendsActivity", "Sending friend request to: $friendId")
                val result = firebaseService.sendFriendRequest(currentUserId, friendId)
                Log.d("FriendsActivity", "Send friend request result: $result")
                
                if (result.isSuccess) {
                    runOnUiThread {
                        callback()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    Log.e("FriendsActivity", "Failed to send friend request: $error")
                    runOnUiThread {
                        Toast.makeText(this@FriendsActivity, "İstek gönderilemedi: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendsActivity", "Exception sending friend request", e)
                runOnUiThread {
                    Toast.makeText(this@FriendsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openChatWithFriend(friend: Friend) {
        // Create or get existing chat
        lifecycleScope.launch {
            try {
                val friendUser = getFriendUser(friend.name)
                if (friendUser != null) {
                    val chatResult = firebaseService.createChat(currentUserId, friendUser.id)
                    if (chatResult.isSuccess) {
                        val chatId = chatResult.getOrNull() ?: ""
                        val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                        intent.putExtra("chat_partner", friend.name)
                        intent.putExtra("chat_id", chatId)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendsActivity, "Sohbet açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getFriendUser(friendName: String): com.selimqueengh.sohbet.models.User? {
        val result = firebaseService.getUsers()
        if (result.isSuccess) {
            val users = result.getOrNull() ?: emptyList()
            return users.find { it.username == friendName }
        }
        return null
    }

    private fun loadFriendsFromFirebase() {
        if (currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getFriends(currentUserId)
                    if (result.isSuccess) {
                        val friends = result.getOrNull() ?: emptyList()
                        friendsList.clear()
                        
                        friends.forEach { friendUser ->
                            val friend = Friend(
                                name = friendUser.username,
                                status = if (friendUser.isOnline) "Online" else "Offline",
                                avatar = "avatar_default"
                            )
                            friendsList.add(friend)
                        }
                        
                        friendsAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@FriendsActivity, "Arkadaşlar yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FriendsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.friends_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_friend_requests -> {
                val intent = Intent(this, FriendRequestsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
