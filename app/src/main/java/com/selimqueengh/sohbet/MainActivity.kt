package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selimqueengh.sohbet.fragments.ChatsFragment
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

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
        
        // Kullanıcıyı online yap
        setUserOnline()
    }
    
    override fun onResume() {
        super.onResume()
        setUserOnline()
    }
    
    override fun onPause() {
        super.onPause()
        setUserOffline()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        setUserOffline()
    }
    
    private fun setUserOnline() {
        val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
        if (realCurrentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                firebaseService.setUserOnline(realCurrentUserId)
            }
        }
    }
    
    private fun setUserOffline() {
        val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
        if (realCurrentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                firebaseService.setUserOffline(realCurrentUserId)
            }
        }
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
        
        // Default to chats fragment
        showChatsFragment()
    }

    private fun showChatsFragment() {
        val fragment = ChatsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }
    


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Menu removed as requested
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
    
    // ===== ONLINE/OFFLINE DURUMU YÖNETİMİ =====
    
    private fun setUserOnline() {
        val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
        if (realCurrentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                firebaseService.setUserOnline(realCurrentUserId)
            }
        }
    }
    
    private fun setUserOffline() {
        val realCurrentUserId = firebaseService.getCurrentUser()?.uid ?: currentUserId
        if (realCurrentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                firebaseService.setUserOffline(realCurrentUserId)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Uygulama ön plana geldiğinde online yap
        setUserOnline()
    }
    
    override fun onPause() {
        super.onPause()
        // Uygulama arka plana geçtiğinde offline yap
        setUserOffline()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Uygulama kapatıldığında offline yap
        setUserOffline()
    }
}