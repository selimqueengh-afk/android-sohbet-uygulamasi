package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseService: FirebaseService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        firebaseService = FirebaseService()
        
        // Daha önce giriş yapılmış mı kontrol et
        checkExistingLogin()
        
        initViews()
        setupAnimations()
        setupListeners()
    }
    
    private fun checkExistingLogin() {
        val savedUsername = sharedPreferences.getString("username", null)
        if (savedUsername != null && savedUsername.isNotEmpty()) {
            // Check if user is already authenticated with Firebase
            if (firebaseService.getCurrentUser() != null) {
                startMainActivity(savedUsername)
            } else {
                // Try to authenticate with Firebase
                lifecycleScope.launch {
                    try {
                        val result = firebaseService.signInWithUsername(savedUsername)
                        if (result.isSuccess) {
                            startMainActivity(savedUsername)
                        } else {
                            // Clear saved data and show login screen
                            sharedPreferences.edit().clear().apply()
                        }
                    } catch (e: Exception) {
                        // Clear saved data and show login screen
                        sharedPreferences.edit().clear().apply()
                    }
                }
            }
        }
    }
    
    private fun initViews() {
        usernameLayout = findViewById(R.id.usernameLayout)
        usernameEditText = findViewById(R.id.usernameEditText)
        loginButton = findViewById(R.id.loginButton)
    }
    
    private fun setupAnimations() {
        // Fade in animasyonu
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        
        // Views'leri animasyonla göster
        usernameLayout.startAnimation(fadeIn)
        loginButton.startAnimation(fadeIn)
    }
    
    private fun setupListeners() {
        usernameEditText.addTextChangedListener { text ->
            val username = text?.toString()?.trim() ?: ""
            loginButton.isEnabled = username.length >= 3
            
            if (username.length < 3 && username.isNotEmpty()) {
                usernameLayout.error = "Kullanıcı adı en az 3 karakter olmalıdır"
            } else {
                usernameLayout.error = null
            }
        }
        
        loginButton.setOnClickListener {
            login()
        }
    }
    
    private fun login() {
        val username = usernameEditText.text?.toString()?.trim() ?: ""
        
        if (username.length >= 3) {
            loginButton.isEnabled = false
            loginButton.text = "Giriş yapılıyor..."
            
            lifecycleScope.launch {
                try {
                    val result = firebaseService.signInWithUsername(username)
                    if (result.isSuccess) {
                        // Kullanıcı adını kaydet
                        sharedPreferences.edit()
                            .putString("username", username)
                            .apply()
                        
                        // Ana ekrana geç
                        startMainActivity(username)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Giriş başarısız: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                            loginButton.text = "Giriş Yap"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Giriş hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                }
            }
        }
    }
    
    private fun startMainActivity(username: String) {
        val intent = Intent(this, MainActivityNew::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
        
        // Geçiş animasyonu
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}