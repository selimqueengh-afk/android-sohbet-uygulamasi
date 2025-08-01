package com.selimqueengh.sohbet

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.launch

class TestUsersActivity : AppCompatActivity() {
    
    private lateinit var firebaseService: FirebaseService
    private lateinit var resultText: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_users)

        firebaseService = FirebaseService()
        resultText = findViewById(R.id.resultText)
        testButton = findViewById(R.id.testButton)

        testButton.setOnClickListener {
            listAllUsers()
        }
    }

    private fun listAllUsers() {
        lifecycleScope.launch {
            try {
                resultText.text = "Kullanıcılar listeleniyor..."
                
                val result = firebaseService.getUsers()
                Log.d("TestUsersActivity", "getUsers result: $result")
                
                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    Log.d("TestUsersActivity", "Found ${users.size} users")
                    
                    val userList = users.joinToString("\n") { user ->
                        "• ${user.username} (ID: ${user.id})"
                    }
                    
                    resultText.text = """
                        📋 Firebase'deki Kullanıcılar (${users.size} adet):
                        
                        $userList
                        
                        💡 Test için:
                        1. İlk kullanıcı: test1
                        2. İkinci kullanıcı: test2
                    """.trimIndent()
                    
                    Toast.makeText(this@TestUsersActivity, "${users.size} kullanıcı bulundu", Toast.LENGTH_SHORT).show()
                    
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                    Log.e("TestUsersActivity", "Failed to get users: $error")
                    resultText.text = "❌ Hata: $error"
                    Toast.makeText(this@TestUsersActivity, "Kullanıcılar yüklenemedi: $error", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("TestUsersActivity", "Error listing users", e)
                resultText.text = "❌ Hata: ${e.message}"
                Toast.makeText(this@TestUsersActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}