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
import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.models.UserStatus
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
            // Firebase authentication kontrolü
            val currentUser = firebaseService.getCurrentUser()
            if (currentUser != null) {
                startMainActivity(savedUsername)
            } else {
                // Firebase authentication yoksa, yeniden giriş yap
                sharedPreferences.edit().remove("username").apply()
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
        
        // Slide up animasyonu
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideUp.duration = 800
        
        // Views'leri animasyonla göster
        usernameLayout.startAnimation(fadeIn)
        loginButton.startAnimation(slideUp)
        
        // Logo animasyonu
        val logo = findViewById<android.widget.ImageView>(R.id.logoImageView)
        logo?.let {
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in)
            scaleAnimation.duration = 1200
            it.startAnimation(scaleAnimation)
        }
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
                    // Firebase anonymous authentication
                    val authResult = firebaseService.signInAnonymously()
                    
                    if (authResult.isSuccess) {
                        val currentUser = authResult.getOrNull()
                        if (currentUser != null) {
                            // Create user in Firestore
                            val user = User(
                                username = username,
                                displayName = username,
                                status = UserStatus.ONLINE,
                                isOnline = true
                            )
                            
                            val createUserResult = firebaseService.createUser(user)
                            if (createUserResult.isSuccess) {
                                // Update FCM token
                                firebaseService.updateFCMToken(currentUser.uid)
                                
                                // Kullanıcı adını kaydet
                                sharedPreferences.edit()
                                    .putString("username", username)
                                    .putString("user_id", currentUser.uid)
                                    .apply()
                                
                                // Ana ekrana geç
                                startMainActivity(username)
                            } else {
                                Toast.makeText(this@LoginActivity, "Kullanıcı oluşturulamadı", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                                loginButton.text = "Giriş Yap"
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Authentication başarısız", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                            loginButton.text = "Giriş Yap"
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Giriş başarısız: ${authResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginButton.text = "Giriş Yap"
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                    loginButton.text = "Giriş Yap"
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