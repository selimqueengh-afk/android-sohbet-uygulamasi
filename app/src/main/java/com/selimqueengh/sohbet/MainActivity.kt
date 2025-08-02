package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.selimqueengh.sohbet.fragments.ChatsFragment
import com.selimqueengh.sohbet.services.FirebaseService

class MainActivity : AppCompatActivity() {
    
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
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
        setupClickListeners()
        
        // Check for updates first
        checkForUpdates()
        
        // Default to chats fragment
        showChatsFragment()
    }

    private fun initViews() {
        // Toolbar'ı ayarla
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupClickListeners() {
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_chats -> {
                    showChatsFragment()
                    true
                }
                R.id.nav_friends -> {
                    val intent = Intent(this, FriendsActivity::class.java)
                    startActivityForResult(intent, 1001)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, TestUsersActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun showChatsFragment() {
        val fragment = ChatsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }
    
    private fun checkForUpdates() {
        firebaseService.checkForUpdates { hasUpdate, updateUrl, forceUpdate ->
            runOnUiThread {
                if (hasUpdate) {
                    showUpdateDialog(updateUrl, forceUpdate)
                }
            }
        }
    }
    
    private fun showUpdateDialog(updateUrl: String, forceUpdate: Boolean) {
        val dialogBuilder = AlertDialog.Builder(this)
        
        if (forceUpdate) {
            dialogBuilder.setTitle("Güncelleme Gerekli")
                .setMessage("Uygulamanın yeni bir sürümü mevcut. Devam etmek için güncelleme yapmanız gerekiyor.")
                .setCancelable(false)
                .setPositiveButton("Güncelle") { _, _ ->
                    // Open update URL
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Güncelleme linki açılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Çıkış") { _, _ ->
                    finish()
                }
        } else {
            dialogBuilder.setTitle("Güncelleme Mevcut")
                .setMessage("Uygulamanın yeni bir sürümü mevcut. Güncellemek ister misiniz?")
                .setCancelable(true)
                .setPositiveButton("Güncelle") { _, _ ->
                    // Open update URL
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Güncelleme linki açılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Daha Sonra") { dialog, _ ->
                    dialog.dismiss()
                }
        }
        
        dialogBuilder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            // Refresh chats when returning from friend requests
            showChatsFragment()
        }
    }
}