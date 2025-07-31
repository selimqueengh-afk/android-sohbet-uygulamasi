package com.selimqueengh.sohbet

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        
        // Daha önce giriş yapılmış mı kontrol et
        checkExistingLogin()
        
        initViews()
        setupAnimations()
        setupListeners()
    }
    
    private fun checkExistingLogin() {
        val savedUsername = sharedPreferences.getString("username", null)
        if (savedUsername != null && savedUsername.isNotEmpty()) {
            // Kullanıcı daha önce giriş yapmış, direkt ana ekrana git
            startMainActivity(savedUsername)
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
            // Kullanıcı adını kaydet
            sharedPreferences.edit()
                .putString("username", username)
                .apply()
            
            // Ana ekrana geç
            startMainActivity(username)
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