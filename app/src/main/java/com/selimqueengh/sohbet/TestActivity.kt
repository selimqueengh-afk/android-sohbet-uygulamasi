package com.selimqueengh.sohbet

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class TestActivity : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var resultText: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        firestore = FirebaseFirestore.getInstance()
        resultText = findViewById(R.id.resultText)
        testButton = findViewById(R.id.testButton)

        testButton.setOnClickListener {
            testFirestore()
        }
    }

    private fun testFirestore() {
        lifecycleScope.launch {
            try {
                // 1. Users koleksiyonunu test et
                val usersSnapshot = firestore.collection("users").get().await()
                Log.d("TestActivity", "Users count: ${usersSnapshot.size()}")

                // 2. Friend requests koleksiyonunu test et
                val requestsSnapshot = firestore.collection("friend_requests").get().await()
                Log.d("TestActivity", "Friend requests count: ${requestsSnapshot.size()}")

                // 3. Chats koleksiyonunu test et
                val chatsSnapshot = firestore.collection("chats").get().await()
                Log.d("TestActivity", "Chats count: ${chatsSnapshot.size()}")

                // 4. Messages koleksiyonunu test et
                val messagesSnapshot = firestore.collection("messages").get().await()
                Log.d("TestActivity", "Messages count: ${messagesSnapshot.size()}")

                // 5. Test verisi ekle
                val testData = hashMapOf(
                    "test" to "data",
                    "timestamp" to Date()
                )
                
                firestore.collection("test").add(testData).await()
                Log.d("TestActivity", "Test data added successfully")

                runOnUiThread {
                    resultText.text = """
                        Users: ${usersSnapshot.size()}
                        Friend Requests: ${requestsSnapshot.size()}
                        Chats: ${chatsSnapshot.size()}
                        Messages: ${messagesSnapshot.size()}
                        Test data added successfully
                    """.trimIndent()
                    
                    Toast.makeText(this@TestActivity, "Test tamamlandı!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("TestActivity", "Test error", e)
                runOnUiThread {
                    resultText.text = "Hata: ${e.message}"
                    Toast.makeText(this@TestActivity, "Test hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}