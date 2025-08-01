package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.services.FirebaseRealtimeService
import kotlinx.coroutines.launch

class FriendRequestsActivityRealtime : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var firebaseService: FirebaseRealtimeService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var requestsAdapter: FriendRequestsAdapter
    
    private val requestsList = mutableListOf<FriendRequest>()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseRealtimeService()
        
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
                "accept" -> acceptFriendRequest(request.senderId)
                "reject" -> rejectFriendRequest(request.senderId)
            }
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FriendRequestsActivityRealtime)
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
                    val result = firebaseService.getIncomingFriendRequests(currentUserId)
                    if (result.isSuccess) {
                        val requestsData = result.getOrNull() ?: emptyList()
                        requestsList.clear()
                        
                        // Her istek için kullanıcı bilgilerini al
                        for (requestData in requestsData) {
                            val senderId = requestData["senderId"] as? String ?: ""
                            val timestamp = requestData["timestamp"] as? Long ?: 0L
                            
                            // Gönderen kullanıcının bilgilerini al
                            val userResult = firebaseService.searchUserByUsername("") // Bu kısmı düzeltmek gerekiyor
                            if (userResult.isSuccess) {
                                val senderUser = userResult.getOrNull()
                                if (senderUser != null) {
                                    val request = FriendRequest(
                                        requestId = senderId, // requestId olarak senderId kullanıyoruz
                                        senderId = senderId,
                                        senderUsername = senderUser.username,
                                        timestamp = timestamp
                                    )
                                    requestsList.add(request)
                                }
                            }
                        }
                        
                        requestsAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@FriendRequestsActivityRealtime, "İstekler yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FriendRequestsActivityRealtime, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun acceptFriendRequest(senderId: String) {
        lifecycleScope.launch {
            try {
                val result = firebaseService.acceptFriendRequest(currentUserId, senderId)
                if (result.isSuccess) {
                    Toast.makeText(this@FriendRequestsActivityRealtime, "İstek kabul edildi", Toast.LENGTH_SHORT).show()
                    loadFriendRequests() // Listeyi yenile
                } else {
                    Toast.makeText(this@FriendRequestsActivityRealtime, "İstek kabul edilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivityRealtime, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectFriendRequest(senderId: String) {
        lifecycleScope.launch {
            try {
                val result = firebaseService.rejectFriendRequest(currentUserId, senderId)
                if (result.isSuccess) {
                    Toast.makeText(this@FriendRequestsActivityRealtime, "İstek reddedildi", Toast.LENGTH_SHORT).show()
                    loadFriendRequests() // Listeyi yenile
                } else {
                    Toast.makeText(this@FriendRequestsActivityRealtime, "İstek reddedilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendRequestsActivityRealtime, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
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