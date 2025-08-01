package com.selimqueengh.sohbet.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        private const val FRIENDS_COLLECTION = "friends"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    // Authentication methods
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

    // User management
    suspend fun createUser(user: User): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userData = hashMapOf(
                    "id" to currentUser.uid,
                    "username" to user.username,
                    "displayName" to user.displayName,
                    "avatarUrl" to user.avatarUrl,
                    "status" to user.status.name,
                    "lastSeen" to user.lastSeen,
                    "isOnline" to user.isOnline,
                    "createdAt" to Date()
                )
                firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .set(userData)
                    .await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No authenticated user"))
            }
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
                "lastSeen" to Date()
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

    // Chat and messaging
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
            
            // Update chat's last message
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
                "lastMessageId" to message.id,
                "lastMessageContent" to message.content,
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

    suspend fun createChat(user1Id: String, user2Id: String): Result<String> {
        return try {
            val chatData = hashMapOf(
                "user1Id" to user1Id,
                "user2Id" to user2Id,
                "createdAt" to Date(),
                "updatedAt" to Date()
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
                .whereEqualTo("user1Id", userId)
                .get()
                .await()
            
            val snapshot2 = firestore.collection(CHATS_COLLECTION)
                .whereEqualTo("user2Id", userId)
                .get()
                .await()
            
            val allChats = snapshot.documents + snapshot2.documents
            val chats = allChats.map { doc ->
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

    // Friends management
    suspend fun addFriend(userId: String, friendId: String): Result<Unit> {
        return try {
            val friendData = hashMapOf(
                "userId" to userId,
                "friendId" to friendId,
                "status" to "pending",
                "createdAt" to Date()
            )
            
            firestore.collection(FRIENDS_COLLECTION)
                .add(friendData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding friend", e)
            Result.failure(e)
        }
    }

    suspend fun getFriends(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()
            
            val friends = snapshot.documents.map { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("friendId", doc.id)
                } ?: mutableMapOf()
            }
            
            Result.success(friends)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friends", e)
            Result.failure(e)
        }
    }

    suspend fun getFriendRequests(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("friendId", userId)
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
            Log.e(TAG, "Error getting friend requests", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingFriendRequests(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
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
            Log.e(TAG, "Error getting pending friend requests", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIENDS_COLLECTION)
                .document(requestId)
                .update("status", "accepted")
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIENDS_COLLECTION)
                .document(requestId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            Result.failure(e)
        }
    }

    // FCM Token management
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

    // Real-time listeners
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