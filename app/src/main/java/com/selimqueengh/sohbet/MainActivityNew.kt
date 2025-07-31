package com.selimqueengh.sohbet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.selimqueengh.sohbet.fragments.ChatsFragment
import com.selimqueengh.sohbet.fragments.FriendsFragment
import com.selimqueengh.sohbet.fragments.ProfileFragment
import com.selimqueengh.sohbet.fragments.SettingsFragment

class MainActivityNew : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        saveUsername(currentUsername)
        
        setupToolbar()
        setupBottomNavigation()
        
        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(ChatsFragment())
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "SuperChat"
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    replaceFragment(ChatsFragment())
                    supportActionBar?.title = "Sohbetler"
                    true
                }
                R.id.nav_friends -> {
                    replaceFragment(FriendsFragment())
                    supportActionBar?.title = "Arkadaşlar"
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    supportActionBar?.title = "Ayarlar"
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    supportActionBar?.title = "Profil"
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    private fun saveUsername(username: String) {
        val sharedPref = getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                bottomNavigation.selectedItemId = R.id.nav_profile
                true
            }
            R.id.action_settings -> {
                // TODO: Implement settings
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        // If not on chats tab, go to chats tab
        if (bottomNavigation.selectedItemId != R.id.nav_chats) {
            bottomNavigation.selectedItemId = R.id.nav_chats
        } else {
            super.onBackPressed()
        }
    }
}
