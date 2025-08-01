package com.selimqueengh.sohbet

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selimqueengh.sohbet.services.FirebaseRealtimeService
import kotlinx.coroutines.launch

class FriendsActivityRealtime : AppCompatActivity() {
    
    private lateinit var firebaseService: FirebaseRealtimeService
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseRealtimeService()
        
        // Arkadaş ekleme butonu
        findViewById<Button>(R.id.addFriendButton).setOnClickListener {
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
        
        // Kullanıcı Arama
        searchButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            if (username.isNotEmpty()) {
                searchButton.text = "Aranıyor..."
                searchButton.isEnabled = false
                
                lifecycleScope.launch {
                    try {
                        val result = firebaseService.searchUserByUsername(username)
                        if (result.isSuccess) {
                            val user = result.getOrNull()
                            
                            runOnUiThread {
                                if (user != null && user.id != currentUserId) {
                                    foundUser = user
                                    searchButton.text = "✓ Kullanıcı Bulundu"
                                    searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                                    sendRequestButton.visibility = android.view.View.VISIBLE
                                    Log.d("FriendsActivity", "Found user: ${user.username}")
                                } else {
                                    searchButton.text = "✗ Kullanıcı Bulunamadı"
                                    searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                                    sendRequestButton.visibility = android.view.View.GONE
                                    Log.d("FriendsActivity", "User not found or is current user")
                                }
                                searchButton.isEnabled = true
                            }
                        } else {
                            runOnUiThread {
                                searchButton.text = "✗ Hata"
                                searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                                sendRequestButton.visibility = android.view.View.GONE
                                searchButton.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            searchButton.text = "✗ Hata"
                            searchButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                            sendRequestButton.visibility = android.view.View.GONE
                            searchButton.isEnabled = true
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Kullanıcı adı girin", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Arkadaşlık İsteği Gönderme
        sendRequestButton.setOnClickListener {
            foundUser?.let { user ->
                lifecycleScope.launch {
                    try {
                        val result = firebaseService.sendFriendRequest(currentUserId, user.id)
                        if (result.isSuccess) {
                            runOnUiThread {
                                dialog.dismiss()
                                Toast.makeText(this@FriendsActivityRealtime, "Arkadaşlık isteği gönderildi", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@FriendsActivityRealtime, "İstek gönderilemedi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@FriendsActivityRealtime, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        dialog.show()
    }
}