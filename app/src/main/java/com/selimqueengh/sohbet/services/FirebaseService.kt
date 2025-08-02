package com.selimqueengh.sohbet.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.models.UserStatus
import com.selimqueengh.sohbet.models.ChatMessage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FirebaseService {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    
    companion object {
        private const val TAG = "FirebaseService"
        private const val USERS_COLLECTION = "users"
        private const val MESSAGES_COLLECTION = "messages"
        private const val CHATS_COLLECTION = "chats"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        
        // Remote Config keys
        private const val REMOTE_CONFIG_APP_VERSION = "app_version"
        private const val REMOTE_CONFIG_FORCE_UPDATE = "force_update"
        private const val REMOTE_CONFIG_UPDATE_URL = "update_url"
        private const val REMOTE_CONFIG_MAINTENANCE_MODE = "maintenance_mode"
    }
    
    init {
        setupRemoteConfig()
    }
    
    private fun setupRemoteConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 saat
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Default values
        remoteConfig.setDefaultsAsync(mapOf(
            REMOTE_CONFIG_APP_VERSION to "1.0.0",
            REMOTE_CONFIG_FORCE_UPDATE to false,
            REMOTE_CONFIG_UPDATE_URL to "https://play.google.com/store/apps/details?id=com.selimqueengh.sohbet",
            REMOTE_CONFIG_MAINTENANCE_MODE to false
        ))
        
        // Fetch and activate
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Remote config updated successfully")
                checkForUpdates()
            } else {
                Log.e(TAG, "Remote config update failed", task.exception)
            }
        }
    }
    
    fun checkForUpdates(onUpdateAvailable: (Boolean, String, Boolean) -> Unit) {
        val currentVersion = "1.0.0" // BuildConfig.VERSION_NAME
        val remoteVersion = remoteConfig.getString(REMOTE_CONFIG_APP_VERSION)
        val forceUpdate = remoteConfig.getBoolean(REMOTE_CONFIG_FORCE_UPDATE)
        val updateUrl = remoteConfig.getString(REMOTE_CONFIG_UPDATE_URL)
        val maintenanceMode = remoteConfig.getBoolean(REMOTE_CONFIG_MAINTENANCE_MODE)
        
        Log.d(TAG, "Current version: $currentVersion, Remote version: $remoteVersion")
        
        if (maintenanceMode) {
            onUpdateAvailable(true, updateUrl, true)
            return
        }
        
        if (currentVersion != remoteVersion) {
            onUpdateAvailable(true, updateUrl, forceUpdate)
        } else {
            onUpdateAvailable(false, "", false)
        }
    }
    
    fun getMaintenanceMode(): Boolean {
        return remoteConfig.getBoolean(REMOTE_CONFIG_MAINTENANCE_MODE)
    }
    
    fun getUpdateUrl(): String {
        return remoteConfig.getString(REMOTE_CONFIG_UPDATE_URL)
    }

    // ===== AUTHENTICATION =====
    
    fun getCurrentUser() = auth.currentUser
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val userData = hashMapOf(
                    "username" to username,
                    "displayName" to displayName,
                    "email" to email,
                    "createdAt" to Date(),
                    "status" to UserStatus.ONLINE.name,
                    "isOnline" to true,
                    "lastSeen" to Date(),
                    "friends" to emptyList<String>(),
                    "friendRequests" to emptyList<String>()
                )
                
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(userData)
                    .await()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }

    // ===== USER MANAGEMENT =====
    
    suspend fun searchUserByUsername(username: String): Result<User?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val document = snapshot.documents[0]
                val user = User(
                    id = document.id,
                    username = document.getString("username") ?: "",
                    displayName = document.getString("displayName") ?: "",
                    avatarUrl = document.getString("avatarUrl"),
                    status = UserStatus.valueOf(document.getString("status") ?: "OFFLINE"),
                    lastSeen = document.get("lastSeen"),
                    isOnline = document.getBoolean("isOnline") ?: false,
                    isTyping = document.getBoolean("isTyping") ?: false,
                    typingTo = document.getString("typingTo")
                )
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(userId: String, displayName: String, avatarUrl: String?): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "displayName" to displayName
            )
            if (avatarUrl != null) {
                updates["avatarUrl"] = avatarUrl
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            Result.failure(e)
        }
    }

    // ===== FRIEND SYSTEM =====
    
    suspend fun sendFriendRequest(fromUserId: String, toUsername: String): Result<Unit> {
        return try {
            val toUserResult = searchUserByUsername(toUsername)
            if (toUserResult.isSuccess) {
                val toUser = toUserResult.getOrNull()
                if (toUser != null) {
                    val requestData = hashMapOf(
                        "fromUserId" to fromUserId,
                        "toUserId" to toUser.id,
                        "status" to "PENDING",
                        "createdAt" to Date()
                    )
                    
                    firestore.collection(FRIEND_REQUESTS_COLLECTION)
                        .add(requestData)
                        .await()
                    
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Kullanıcı bulunamadı"))
                }
            } else {
                Result.failure(Exception("Kullanıcı arama hatası"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }
    
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val requestDoc = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()
            
            if (requestDoc.exists()) {
                val fromUserId = requestDoc.getString("fromUserId") ?: ""
                val toUserId = requestDoc.getString("toUserId") ?: ""
                
                // Update request status
                firestore.collection(FRIEND_REQUESTS_COLLECTION)
                    .document(requestId)
                    .update("status", "ACCEPTED")
                    .await()
                
                // Add to friends list for both users
                firestore.collection(USERS_COLLECTION)
                    .document(fromUserId)
                    .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(toUserId))
                    .await()
                
                firestore.collection(USERS_COLLECTION)
                    .document(toUserId)
                    .update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(fromUserId))
                    .await()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Friend request not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }
    
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", "REJECTED")
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

    suspend fun removeFriend(userId: String, friendId: String): Result<Unit> {
        return try {
            // Her iki kullanıcının friends listesinden çıkar
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(friendId))
                .await()
            
            firestore.collection(USERS_COLLECTION)
                .document(friendId)
                .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
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

    suspend fun sendMessage(chatId: String, senderId: String, senderUsername: String, content: String): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderUsername" to senderUsername,
                "content" to content,
                "messageType" to "text",
                "timestamp" to Date(),
                "isRead" to false,
                "isDelivered" to false
            )
            
            firestore.collection(MESSAGES_COLLECTION)
                .add(messageData)
                .await()
            
            // Chat'in son mesajını güncelle
            updateChatLastMessage(chatId, content)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }
    
    suspend fun sendMediaMessage(chatId: String, senderId: String, senderUsername: String, mediaUrl: String, mediaType: String): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderUsername" to senderUsername,
                "content" to when {
                    mediaType.startsWith("image/") -> "📷 Resim"
                    mediaType.startsWith("video/") -> "🎥 Video"
                    mediaType.startsWith("audio/") -> "🎵 Ses"
                    else -> "📎 Dosya"
                },
                "messageType" to when {
                    mediaType.startsWith("image/") -> "image"
                    mediaType.startsWith("video/") -> "video"
                    mediaType.startsWith("audio/") -> "audio"
                    else -> "file"
                },
                "mediaUrl" to mediaUrl,
                "mediaType" to mediaType,
                "timestamp" to Date(),
                "isRead" to false,
                "isDelivered" to false
            )
            
            firestore.collection(MESSAGES_COLLECTION)
                .add(messageData)
                .await()
            
            // Chat'in son mesajını güncelle
            updateChatLastMessage(chatId, when {
                mediaType.startsWith("image/") -> "📷 Resim"
                mediaType.startsWith("video/") -> "🎥 Video"
                mediaType.startsWith("audio/") -> "🎵 Ses"
                else -> "📎 Dosya"
            })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending media message", e)
            Result.failure(e)
        }
    }

    private suspend fun updateChatLastMessage(chatId: String, content: String) {
        try {
            val chatData = hashMapOf<String, Any>(
                "lastMessage" to content,
                "lastMessageTimestamp" to Date(),
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
            val snapshot = try {
                firestore.collection(MESSAGES_COLLECTION)
                    .whereEqualTo("chatId", chatId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed with orderBy, trying without: ${e.message}")
                firestore.collection(MESSAGES_COLLECTION)
                    .whereEqualTo("chatId", chatId)
                    .limit(limit)
                    .get()
                    .await()
            }
            
            val messages = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    val timestamp = when (val ts = data["timestamp"]) {
                        is Date -> ts.time
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        is Long -> ts
                        else -> System.currentTimeMillis()
                    }
                    
                    ChatMessage(
                        id = doc.id,
                        chatId = data["chatId"] as? String ?: "",
                        senderId = data["senderId"] as? String ?: "",
                        senderUsername = data["senderUsername"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        messageType = data["messageType"] as? String ?: "text",
                        mediaData = data["mediaData"] as? String ?: "",
                        mediaType = data["mediaType"] as? String ?: "",
                        timestamp = timestamp,
                        isRead = data["isRead"] as? Boolean ?: false,
                        isDelivered = data["isDelivered"] as? Boolean ?: false
                    )
                } else null
            }.sortedBy { it.timestamp }.reversed()
            
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            Result.failure(e)
        }
    }

    fun listenToMessages(chatId: String, onMessage: (ChatMessage) -> Unit) {
        firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to messages", e)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val data = change.document.data
                            if (data != null) {
                                val timestamp = when (val ts = data["timestamp"]) {
                                    is Date -> ts.time
                                    is com.google.firebase.Timestamp -> ts.toDate().time
                                    is Long -> ts
                                    else -> System.currentTimeMillis()
                                }
                                
                                val chatMessage = ChatMessage(
                                    id = change.document.id,
                                    chatId = data["chatId"] as? String ?: "",
                                    senderId = data["senderId"] as? String ?: "",
                                    senderUsername = data["senderUsername"] as? String ?: "",
                                    content = data["content"] as? String ?: "",
                                    messageType = data["messageType"] as? String ?: "text",
                                    mediaData = data["mediaData"] as? String ?: "",
                                    mediaType = data["mediaType"] as? String ?: "",
                                    timestamp = timestamp,
                                    isRead = data["isRead"] as? Boolean ?: false,
                                    isDelivered = data["isDelivered"] as? Boolean ?: false
                                )
                                onMessage(chatMessage)
                            }
                        }
                        else -> {}
                    }
                }
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

    // ===== ONLINE/OFFLINE DURUMU YÖNETİMİ =====
    
    suspend fun setUserOnline(userId: String) {
        try {
            val userData = hashMapOf<String, Any>(
                "status" to "ONLINE",
                "isOnline" to true,
                "lastSeen" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userData)
                .await()
            
            Log.d(TAG, "User $userId set to ONLINE")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user online", e)
        }
    }
    
    suspend fun setUserOffline(userId: String) {
        try {
            val userData = hashMapOf<String, Any>(
                "status" to "OFFLINE",
                "isOnline" to false,
                "lastSeen" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userData)
                .await()
            
            Log.d(TAG, "User $userId set to OFFLINE")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user offline", e)
        }
    }
    
    suspend fun updateLastSeen(userId: String) {
        try {
            val userData = hashMapOf<String, Any>(
                "lastSeen" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userData)
                .await()
            
            Log.d(TAG, "Updated last seen for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last seen", e)
        }
    }
    
    suspend fun getUserStatus(userId: String): Result<User> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val data = document.data
                if (data != null) {
                    val user = User(
                        id = document.id,
                        username = data["username"] as? String ?: "",
                        displayName = data["displayName"] as? String ?: "",
                        avatarUrl = data["avatarUrl"] as? String,
                        status = UserStatus.valueOf(data["status"] as? String ?: "OFFLINE"),
                        lastSeen = data["lastSeen"],
                        isOnline = data["isOnline"] as? Boolean ?: false,
                        isTyping = data["isTyping"] as? Boolean ?: false,
                        typingTo = data["typingTo"] as? String
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data is null"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user status", e)
            Result.failure(e)
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
                
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val user = User(
                            id = snapshot.id,
                            username = data["username"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            avatarUrl = data["avatarUrl"] as? String,
                            status = UserStatus.valueOf(data["status"] as? String ?: "OFFLINE"),
                            lastSeen = data["lastSeen"],
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            isTyping = data["isTyping"] as? Boolean ?: false,
                            typingTo = data["typingTo"] as? String
                        )
                        onStatusChange(user)
                    }
                }
            }
    }
    
    suspend fun setUserTyping(userId: String, isTyping: Boolean, chatId: String) {
        try {
            val userData = hashMapOf<String, Any>(
                "isTyping" to isTyping,
                "typingTo" to if (isTyping) chatId else ""
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user typing", e)
        }
    }
    
    fun listenToUserTyping(userId: String, onTypingChange: (Boolean, String) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to user typing", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val isTyping = data["isTyping"] as? Boolean ?: false
                        val typingTo = data["typingTo"] as? String ?: ""
                        onTypingChange(isTyping, typingTo)
                    }
                }
            }
    }
}