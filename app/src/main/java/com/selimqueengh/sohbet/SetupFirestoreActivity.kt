package com.selimqueengh.sohbet

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class SetupFirestoreActivity : AppCompatActivity() {
    
    private lateinit var firestore: com.google.firebase.firestore.FirebaseFirestore
    private lateinit var resultText: TextView
    private lateinit var setupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_firestore)

        firestore = Firebase.firestore
        resultText = findViewById(R.id.resultText)
        setupButton = findViewById(R.id.setupButton)

        setupButton.setOnClickListener {
            setupFirestoreCollections()
        }
    }

    private fun setupFirestoreCollections() {
        lifecycleScope.launch {
            try {
                resultText.text = "Firestore koleksiyonları oluşturuluyor..."
                
                // 1. Friend Requests koleksiyonu için örnek veri
                val friendRequestData = hashMapOf(
                    "fromUserId" to "test_user_1",
                    "toUserId" to "test_user_2", 
                    "status" to "pending",
                    "timestamp" to Date()
                )
                
                firestore.collection("friend_requests")
                    .add(friendRequestData)
                    .await()
                
                // 2. Chats koleksiyonu için örnek veri
                val chatData = hashMapOf(
                    "participants" to listOf("test_user_1", "test_user_2"),
                    "lastMessage" to "Merhaba!",
                    "lastMessageTimestamp" to Date(),
                    "createdAt" to Date()
                )
                
                firestore.collection("chats")
                    .add(chatData)
                    .await()
                
                // 3. Messages koleksiyonu için örnek veri
                val messageData = hashMapOf(
                    "chatId" to "test_chat_id",
                    "senderId" to "test_user_1",
                    "message" to "Merhaba!",
                    "timestamp" to Date(),
                    "type" to "text"
                )
                
                firestore.collection("messages")
                    .add(messageData)
                    .await()
                
                // 4. Test koleksiyonu için örnek veri
                val testData = hashMapOf(
                    "test" to "setup_completed",
                    "timestamp" to Date()
                )
                
                firestore.collection("test")
                    .add(testData)
                    .await()
                
                runOnUiThread {
                    resultText.text = """
                        ✅ Firestore Koleksiyonları Oluşturuldu!
                        
                        • friend_requests ✅
                        • chats ✅  
                        • messages ✅
                        • test ✅
                        
                        Artık uygulama çalışabilir!
                    """.trimIndent()
                    
                    Toast.makeText(this@SetupFirestoreActivity, "Koleksiyonlar oluşturuldu!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("SetupFirestoreActivity", "Setup error", e)
                runOnUiThread {
                    resultText.text = "❌ Hata: ${e.message}"
                    Toast.makeText(this@SetupFirestoreActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}