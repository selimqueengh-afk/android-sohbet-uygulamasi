package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class FriendRequestsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var requestsAdapter: FriendRequestsAdapter
    
    private val requestsList = mutableListOf<FriendRequest>()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupRecyclerView()
        setupToolbar()
        
        loadFriendRequests()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewRequests)
    }

    private fun setupRecyclerView() {
        requestsAdapter = FriendRequestsAdapter(requestsList) { request, action ->
            when (action) {
                "accept" -> acceptFriendRequest(request.requestId)
                "reject" -> rejectFriendRequest(request.requestId)
            }
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FriendRequestsActivity)
            adapter = requestsAdapter
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.requestsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Arkadaşlık İstekleri"
    }

    private fun loadFriendRequests() {
        if (currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val result = firebaseService.getFriendRequests(currentUserId)
                    if (result.isSuccess) {
                        val requestsData = result.getOrNull() ?: emptyList()
                        requestsList.clear()
                        
                        // Get user details for each request
                        val userResult = firebaseService.getUsers()
                        if (userResult.isSuccess) {
                            val users = userResult.getOrNull() ?: emptyList()
                            
                            requestsData.forEach { requestData ->
                                val senderId = requestData["userId"] as? String ?: ""
                                val senderUser = users.find { it.id == senderId }
                                
                                if (senderUser != null) {
                                    val request = FriendRequest(
                                        requestId = requestData["requestId"] as? String ?: "",
                                        senderId = senderId,
                                        senderUsername = senderUser.username,
                                        timestamp = (requestData["createdAt"] as? java.util.Date)?.time ?: System.currentTimeMillis()
                                    )
                                    requestsList.add(request)
                                }
                            }
                        }
                        
                        requestsAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@FriendRequestsActivity, "İstekler yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FriendRequestsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun acceptFriendRequest(requestId: String) {
        lifecycleScope.launch {
            try {
                val result = firebaseService.acceptFriendRequest(requestId)
                if (result.isSuccess) {
                    Toast.makeText(this@FriendRequestsActivity, "İstek kabul edildi", Toast.LENGTH_SHORT).show()
                    loadFriendRequests() // Refresh list
                } else {
                    Toast.makeText(this@FriendRequestsActivity, "İstek kabul edilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectFriendRequest(requestId: String) {
        lifecycleScope.launch {
            try {
                val result = firebaseService.rejectFriendRequest(requestId)
                if (result.isSuccess) {
                    Toast.makeText(this@FriendRequestsActivity, "İstek reddedildi", Toast.LENGTH_SHORT).show()
                    loadFriendRequests() // Refresh list
                } else {
                    Toast.makeText(this@FriendRequestsActivity, "İstek reddedilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

data class FriendRequest(
    val requestId: String,
    val senderId: String,
    val senderUsername: String,
    val timestamp: Long
)