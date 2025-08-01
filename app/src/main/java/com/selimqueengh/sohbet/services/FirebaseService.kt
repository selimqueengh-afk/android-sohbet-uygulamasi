package com.selimqueengh.sohbet.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

import com.selimqueengh.sohbet.models.User
import com.selimqueengh.sohbet.models.UserStatus
import com.selimqueengh.sohbet.models.ChatMessage
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
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
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

    suspend fun registerWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering with email/password", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email/password", e)
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
                try {
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing user: ${doc.id}", e)
                    // Fallback: create user manually from document data
                    val data = doc.data
                    if (data != null) {
                        User(
                            id = doc.id,
                            username = data["username"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            avatarUrl = data["avatarUrl"] as? String,
                            status = UserStatus.valueOf(data["status"] as? String ?: "OFFLINE"),
                            lastSeen = data["lastSeen"],
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            isTyping = data["isTyping"] as? Boolean ?: false,
                            typingTo = data["typingTo"] as? String
                        )
                    } else null
                }
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
                try {
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing user: ${doc.id}", e)
                    // Fallback: create user manually from document data
                    val data = doc.data
                    if (data != null) {
                        User(
                            id = doc.id,
                            username = data["username"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            avatarUrl = data["avatarUrl"] as? String,
                            status = UserStatus.valueOf(data["status"] as? String ?: "OFFLINE"),
                            lastSeen = data["lastSeen"],
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            isTyping = data["isTyping"] as? Boolean ?: false,
                            typingTo = data["typingTo"] as? String
                        )
                    } else null
                }
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
            Log.d(TAG, "Sending friend request from $fromUserId to $toUserId")
            
            // Daha önce istek gönderilip gönderilmediğini kontrol et
            val existingRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            Log.d(TAG, "Existing requests found: ${existingRequest.size()}")
            
            if (!existingRequest.isEmpty) {
                Log.d(TAG, "Request already exists")
                return Result.failure(Exception("Bu kullanıcıya zaten istek gönderilmiş"))
            }
            
            val requestData = hashMapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to "pending",
                "timestamp" to Timestamp.now()
            )
            
            Log.d(TAG, "Creating request with data: $requestData")
            
            val docRef = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .add(requestData)
                .await()
            
            Log.d(TAG, "Request created successfully with ID: ${docRef.id}")
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
            
            // Otomatik sohbet oluştur (Realtime Database)
            createRealtimeChat(fromUserId, toUserId)
            
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
            
            // Sohbeti de sil (Realtime Database)
            val chatId = if (userId < friendId) "${userId}_${friendId}" else "${friendId}_${userId}"
            database.getReference("chats").child(chatId).removeValue()
            database.getReference("messages").child(chatId).removeValue()
            
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
                "mediaUrl" to "",
                "mediaType" to "",
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

    suspend fun sendMediaMessage(chatId: String, senderId: String, senderUsername: String, content: String, mediaUrl: String, mediaType: String): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderUsername" to senderUsername,
                "content" to content,
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
            updateChatLastMessage(chatId, content)
            
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
            val snapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val messages = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    ChatMessage(
                        id = doc.id,
                        chatId = data["chatId"] as? String ?: "",
                        senderId = data["senderId"] as? String ?: "",
                        senderUsername = data["senderUsername"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        messageType = data["messageType"] as? String ?: "text",
                        mediaUrl = data["mediaUrl"] as? String ?: "",
                        mediaType = data["mediaType"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Date)?.time ?: 0L,
                        isRead = data["isRead"] as? Boolean ?: false,
                        isDelivered = data["isDelivered"] as? Boolean ?: false
                    )
                } else null
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
                            val data = change.document.data
                            if (data != null) {
                                val chatMessage = ChatMessage(
                                    id = change.document.id,
                                    chatId = data["chatId"] as? String ?: "",
                                    senderId = data["senderId"] as? String ?: "",
                                    senderUsername = data["senderUsername"] as? String ?: "",
                                    content = data["content"] as? String ?: "",
                                    messageType = data["messageType"] as? String ?: "text",
                                    mediaUrl = data["mediaUrl"] as? String ?: "",
                                    mediaType = data["mediaType"] as? String ?: "",
                                    timestamp = (data["timestamp"] as? Date)?.time ?: 0L,
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

    // ===== REALTIME DATABASE MESAJLAŞMA =====
    
    fun sendRealtimeMessage(chatId: String, senderId: String, senderUsername: String, content: String, messageType: String = "text") {
        val messageRef = database.getReference("messages").child(chatId).push()
        val message = hashMapOf(
            "senderId" to senderId,
            "senderUsername" to senderUsername,
            "content" to content,
            "messageType" to messageType,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "isDelivered" to false
        )
        
        messageRef.setValue(message).addOnSuccessListener {
            Log.d(TAG, "Message sent successfully to Realtime Database")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error sending message to Realtime Database", e)
        }
    }

    fun listenToRealtimeMessages(chatId: String, onMessage: (Map<String, Any>) -> Unit) {
        val messagesRef = database.getReference("messages").child(chatId)
        
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.value as? Map<String, Any>
                    message?.let {
                        val messageWithId = it.toMutableMap()
                        messageWithId["id"] = messageSnapshot.key ?: ""
                        onMessage(messageWithId)
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to Realtime Database messages", error.toException())
            }
        })
    }

    fun createRealtimeChat(user1Id: String, user2Id: String): String {
        val chatId = if (user1Id < user2Id) "${user1Id}_${user2Id}" else "${user2Id}_${user1Id}"
        
        val chatRef = database.getReference("chats").child(chatId)
        val chatData = hashMapOf(
            "participants" to listOf(user1Id, user2Id),
            "lastMessage" to "",
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )
        
        chatRef.setValue(chatData)
        return chatId
    }

    fun getRealtimeChats(userId: String, onChats: (List<Map<String, Any>>) -> Unit) {
        val chatsRef = database.getReference("chats")
        
        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<Map<String, Any>>()
                
                for (chatSnapshot in snapshot.children) {
                    val chat = chatSnapshot.value as? Map<String, Any>
                    chat?.let {
                        val participants = it["participants"] as? List<String>
                        if (participants?.contains(userId) == true) {
                            val chatWithId = it.toMutableMap()
                            chatWithId["chatId"] = chatSnapshot.key ?: ""
                            chats.add(chatWithId)
                        }
                    }
                }
                
                onChats(chats)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting Realtime Database chats", error.toException())
                onChats(emptyList())
            }
        })
    }
}