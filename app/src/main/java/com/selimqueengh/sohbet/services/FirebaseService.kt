package com.selimqueengh.sohbet.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.messaging.FirebaseMessaging
import com.selimqueengh.sohbet.models.ChatMessage
import com.selimqueengh.sohbet.models.User
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseService {
    companion object {
        private const val TAG = "FirebaseService"
        private const val USERS_COLLECTION = "users"
        private const val MESSAGES_COLLECTION = "messages"
        private const val CHATS_COLLECTION = "chats"
        private const val FRIEND_REQUESTS_COLLECTION = "friend_requests"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    // ===== AUTHENTICATION =====
    
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in anonymously", e)
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }

    // ===== KULLANICI YÖNETİMİ =====
    
    suspend fun createUser(userId: String, username: String, displayName: String): Result<Unit> {
        return try {
            // Kullanıcı adının benzersiz olup olmadığını kontrol et
            val existingUser = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (!existingUser.isEmpty) {
                return Result.failure(Exception("Bu kullanıcı adı zaten kullanılıyor"))
            }
            
            val userData = hashMapOf(
                "id" to userId,
                "username" to username,
                "displayName" to displayName,
                "avatarUrl" to "",
                "status" to "ONLINE",
                "lastSeen" to Timestamp.now(),
                "isOnline" to true,
                "friends" to listOf<String>(),
                "createdAt" to Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserStatus(userId: String, isOnline: Boolean, status: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "status" to status,
                "lastSeen" to Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user status", e)
        }
    }

    suspend fun getUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users", e)
            Result.failure(e)
        }
    }

    suspend fun searchUserByUsername(username: String): Result<User?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .await()
            
            val user = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user by username", e)
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val user = if (doc.exists()) {
                doc.toObject(User::class.java)?.copy(id = doc.id)
            } else null
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            Result.failure(e)
        }
    }

    // ===== ARKADAŞ SİSTEMİ =====
    
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            // Daha önce istek gönderilip gönderilmediğini kontrol et
            val existingRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Bu kullanıcıya zaten istek gönderilmiş"))
            }
            
            val requestData = hashMapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to "pending",
                "timestamp" to Date()
            )
            
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .add(requestData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }

    suspend fun getIncomingFriendRequests(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            val requests = snapshot.documents.map { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("requestId", doc.id)
                } ?: mutableMapOf()
            }
            
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting incoming friend requests", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            // İsteği kabul et
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", "accepted")
                .await()
            
            // İsteğin detaylarını al
            val requestDoc = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()
            
            val fromUserId = requestDoc.getString("fromUserId") ?: ""
            val toUserId = requestDoc.getString("toUserId") ?: ""
            
            // Her iki kullanıcının friends listesine ekle
            firestore.collection(USERS_COLLECTION)
                .document(toUserId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(fromUserId))
                .await()
            
            firestore.collection(USERS_COLLECTION)
                .document(fromUserId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(toUserId))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            Result.failure(e)
        }
    }

    suspend fun getFriends(userId: String): Result<List<User>> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val friendsList = userDoc.get("friends") as? List<String> ?: emptyList()
            val friends = mutableListOf<User>()
            
            for (friendId in friendsList) {
                val friendDoc = firestore.collection(USERS_COLLECTION)
                    .document(friendId)
                    .get()
                    .await()
                
                if (friendDoc.exists()) {
                    val friendUser = friendDoc.toObject(User::class.java)?.copy(id = friendDoc.id)
                    if (friendUser != null) {
                        friends.add(friendUser)
                    }
                }
            }
            
            Result.success(friends)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friends", e)
            Result.failure(e)
        }
    }

    // ===== SOHBET SİSTEMİ =====
    
    suspend fun createChat(user1Id: String, user2Id: String): Result<String> {
        return try {
            val chatData = hashMapOf(
                "participants" to listOf(user1Id, user2Id),
                "lastMessage" to "",
                "lastMessageTimestamp" to Date(),
                "createdAt" to Date()
            )
            
            val docRef = firestore.collection(CHATS_COLLECTION)
                .add(chatData)
                .await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            Result.failure(e)
        }
    }

    suspend fun getChats(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participants", userId)
                .get()
                .await()
            
            val chats = snapshot.documents.map { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("chatId", doc.id)
                } ?: mutableMapOf()
            }
            
            Result.success(chats)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chats", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "chatId" to message.chatId,
                "senderId" to message.senderId,
                "senderUsername" to message.senderUsername,
                "content" to message.content,
                "messageType" to message.messageType.name,
                "timestamp" to Date(message.timestamp),
                "isRead" to message.isRead,
                "isDelivered" to message.isDelivered,
                "replyToMessageId" to message.replyToMessageId,
                "mediaUrl" to message.mediaUrl,
                "mediaFileName" to message.mediaFileName,
                "isEdited" to message.isEdited,
                "editedAt" to message.editedAt?.let { Date(it) }
            )
            
            firestore.collection(MESSAGES_COLLECTION)
                .add(messageData)
                .await()
            
            // Chat'in son mesajını güncelle
            updateChatLastMessage(message.chatId, message)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    private suspend fun updateChatLastMessage(chatId: String, message: ChatMessage) {
        try {
            val chatData = hashMapOf<String, Any>(
                "lastMessage" to message.content,
                "lastMessageTimestamp" to Date(message.timestamp),
                "updatedAt" to Date()
            )
            
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update(chatData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat last message", e)
        }
    }

    suspend fun getMessages(chatId: String, limit: Long = 50): Result<List<ChatMessage>> {
        return try {
            val snapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }.reversed()
            
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            Result.failure(e)
        }
    }

    // ===== FCM TOKEN YÖNETİMİ =====
    
    suspend fun updateFCMToken(userId: String): Result<String> {
        return try {
            val token = messaging.token.await()
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", token)
                .await()
            
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    // ===== REAL-TIME DİNLEME =====
    
    fun listenToMessages(chatId: String, onMessage: (ChatMessage) -> Unit) {
        firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to messages", e)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val message = change.document.toObject(ChatMessage::class.java)
                            message?.let { onMessage(it.copy(id = change.document.id)) }
                        }
                        else -> {}
                    }
                }
            }
    }

    fun listenToUserStatus(userId: String, onStatusChange: (User) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to user status", e)
                    return@addSnapshotListener
                }
                
                snapshot?.toObject(User::class.java)?.let { user ->
                    onStatusChange(user.copy(id = snapshot.id))
                }
            }
    }
}