package com.selimqueengh.sohbet.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.selimqueengh.sohbet.models.ChatMessage
import com.selimqueengh.sohbet.models.User
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    // Current user
    private var currentUser: FirebaseUser? = null
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
    }
    
    // Authentication methods
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithUsername(username: String): Result<FirebaseUser> {
        return try {
            // For demo purposes, we'll use anonymous auth and store username in Firestore
            val result = auth.signInAnonymously().await()
            val user = result.user!!
            
            // Store user data in Firestore
            val userData = hashMapOf(
                "username" to username,
                "displayName" to username,
                "createdAt" to Date(),
                "lastSeen" to Date(),
                "isOnline" to true
            )
            
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUser(): FirebaseUser? = currentUser
    
    fun signOut() {
        auth.signOut()
    }
    
    // User management
    suspend fun updateUserStatus(isOnline: Boolean) {
        currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            userRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to Date()
                )
            ).await()
        }
    }
    
    suspend fun getUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    User(
                        id = doc.id,
                        username = data["username"] as? String ?: "",
                        displayName = data["displayName"] as? String ?: "",
                        isOnline = data["isOnline"] as? Boolean ?: false,
                        lastSeen = (data["lastSeen"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    )
                } else null
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Chat functionality
    suspend fun sendMessage(chatId: String, content: String, receiverId: String): Result<ChatMessage> {
        return try {
            currentUser?.let { user ->
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = user.uid,
                    senderUsername = user.displayName ?: "Unknown",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(message.id)
                    .set(message)
                    .await()
                
                // Update chat metadata
                firestore.collection("chats")
                    .document(chatId)
                    .set(
                        mapOf(
                            "lastMessage" to content,
                            "lastMessageTime" to Date(),
                            "participants" to listOf(user.uid, receiverId)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
                
                Result.success(message)
            } ?: Result.failure(Exception("User not authenticated"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun listenToMessages(chatId: String, onMessage: (ChatMessage) -> Unit): ListenerRegistration {
        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val message = change.document.toObject(ChatMessage::class.java)
                            message?.let { onMessage(it) }
                        }
                        else -> {}
                    }
                }
            }
    }
    
    suspend fun getChatMessages(chatId: String): Result<List<ChatMessage>> {
        return try {
            val snapshot = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)
            }
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createChat(userId1: String, userId2: String): Result<String> {
        return try {
            val chatId = if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
            
            firestore.collection("chats")
                .document(chatId)
                .set(
                    mapOf(
                        "participants" to listOf(userId1, userId2),
                        "createdAt" to Date()
                    )
                )
                .await()
            
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserChats(): Result<List<String>> {
        return try {
            currentUser?.let { user ->
                val snapshot = firestore.collection("chats")
                    .whereArrayContains("participants", user.uid)
                    .get()
                    .await()
                
                val chatIds = snapshot.documents.map { it.id }
                Result.success(chatIds)
            } ?: Result.failure(Exception("User not authenticated"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // File upload
    suspend fun uploadFile(filePath: String, fileName: String): Result<String> {
        return try {
            val fileRef = storage.reference.child("uploads/$fileName")
            val uploadTask = fileRef.putFile(android.net.Uri.parse(filePath)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}