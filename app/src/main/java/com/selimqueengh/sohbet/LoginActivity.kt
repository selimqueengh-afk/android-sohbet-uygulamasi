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
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
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
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        
        // TextInputEditText'ten text'i almak için
        usernameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonStates()
            }
        })
        
        passwordEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateButtonStates()
            }
        })
    }

    private fun updateButtonStates() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        
        val isValid = username.isNotEmpty() && username.length >= 3 && password.isNotEmpty() && password.length >= 6
        
        loginButton.isEnabled = isValid
        registerButton.isEnabled = isValid
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
            val password = passwordEditText.text.toString().trim()
            
            if (username.isEmpty()) {
                Toast.makeText(this, "Kullanıcı adı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (username.length < 3) {
                Toast.makeText(this, "Kullanıcı adı en az 3 karakter olmalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loginButton.isEnabled = false
            loginButton.text = "Giriş yapılıyor..."
            
            performLogin(username, password)
        }
        
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            if (username.isEmpty()) {
                Toast.makeText(this, "Kullanıcı adı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Şifre girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (username.length < 3) {
                Toast.makeText(this, "Kullanıcı adı en az 3 karakter olmalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            registerButton.isEnabled = false
            registerButton.text = "Kayıt olunuyor..."
            
            performRegister(username, password)
        }
    }

    private fun performLogin(username: String, password: String) {
        lifecycleScope.launch {
            try {
                // Email formatında kullanıcı adı oluştur
                val email = "$username@superchat.com"
                
                // Firebase Auth ile giriş yap
                val authResult = firebaseService.signInWithEmailPassword(email, password)
                if (authResult.isFailure) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Giriş başarısız. Kullanıcı adı veya şifre hatalı.", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                    return@launch
                }
                
                val currentUser = authResult.getOrNull()
                if (currentUser == null) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                    return@launch
                }
                
                // Kullanıcı bilgilerini SharedPreferences'a kaydet
                sharedPreferences.edit().apply {
                    putString("user_id", currentUser.uid)
                    putString("username", username)
                    putString("email", email)
                    apply()
                }
                
                // FCM token güncelle
                firebaseService.updateFCMToken(currentUser.uid)
                
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Hoş geldin, $username!", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                    finish()
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

    private fun performRegister(username: String, password: String) {
        lifecycleScope.launch {
            try {
                // Email formatında kullanıcı adı oluştur
                val email = "$username@superchat.com"
                
                // Firebase Auth ile kayıt ol
                val authResult = firebaseService.registerWithEmailPassword(email, password)
                if (authResult.isFailure) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Kayıt başarısız. Bu kullanıcı adı zaten kullanılıyor olabilir.", Toast.LENGTH_SHORT).show()
                        registerButton.isEnabled = true
                        registerButton.text = "Kayıt Ol"
                    }
                    return@launch
                }
                
                val currentUser = authResult.getOrNull()
                if (currentUser == null) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Kullanıcı oluşturulamadı", Toast.LENGTH_SHORT).show()
                        registerButton.isEnabled = true
                        registerButton.text = "Kayıt Ol"
                    }
                    return@launch
                }
                
                // Kullanıcıyı Firestore'da oluştur
                val createUserResult = firebaseService.createUser(
                    userId = currentUser.uid,
                    username = username,
                    displayName = username
                )
                
                if (createUserResult.isSuccess) {
                    // Kullanıcı bilgilerini SharedPreferences'a kaydet
                    sharedPreferences.edit().apply {
                        putString("user_id", currentUser.uid)
                        putString("username", username)
                        putString("email", email)
                        apply()
                    }
                    
                    // FCM token güncelle
                    firebaseService.updateFCMToken(currentUser.uid)
                    
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Kayıt başarılı! Hoş geldin, $username!", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("username", username)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val errorMessage = createUserResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        registerButton.isEnabled = true
                        registerButton.text = "Kayıt Ol"
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    registerButton.isEnabled = true
                    registerButton.text = "Kayıt Ol"
                }
            }
        }
    }
}