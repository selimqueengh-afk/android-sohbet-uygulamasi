package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch
import java.util.*

class LoginActivity : AppCompatActivity() {
    
    private lateinit var usernameEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseService = FirebaseService()
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        
        initViews()
        setupAnimations()
        setupClickListeners()
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.usernameEditText)
        loginButton = findViewById(R.id.loginButton)
    }

    private fun setupAnimations() {
        // Login butonu animasyonu
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideUp.duration = 1000
        loginButton.startAnimation(slideUp)
        
        // Logo animasyonu
        val logo = findViewById<android.widget.ImageView>(R.id.logoImageView)
        logo?.let {
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in)
            scaleAnimation.duration = 1200
            it.startAnimation(scaleAnimation)
        }
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            
            if (username.isEmpty()) {
                Toast.makeText(this, "Kullanıcı adı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (username.length < 3) {
                Toast.makeText(this, "Kullanıcı adı en az 3 karakter olmalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loginButton.isEnabled = false
            loginButton.text = "Giriş yapılıyor..."
            
            performLogin(username)
        }
    }

    private fun performLogin(username: String) {
        lifecycleScope.launch {
            try {
                // 1. Anonymous authentication
                val authResult = firebaseService.signInAnonymously()
                if (authResult.isFailure) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Authentication hatası", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                    return@launch
                }
                
                val currentUser = authResult.getOrNull()
                if (currentUser == null) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Kullanıcı oluşturulamadı", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                    return@launch
                }
                
                // 2. Kullanıcıyı Firestore'da oluştur
                val createUserResult = firebaseService.createUser(
                    userId = currentUser.uid,
                    username = username,
                    displayName = username
                )
                
                if (createUserResult.isSuccess) {
                    // 3. SharedPreferences'a kaydet
                    sharedPreferences.edit().apply {
                        putString("user_id", currentUser.uid)
                        putString("username", username)
                        apply()
                    }
                    
                    // 4. FCM token güncelle
                    firebaseService.updateFCMToken(currentUser.uid)
                    
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Hoş geldin, $username!", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@LoginActivity, TestActivity::class.java)
                        intent.putExtra("username", username)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val errorMessage = createUserResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                    loginButton.text = "Giriş Yap"
                }
            }
        }
    }
}