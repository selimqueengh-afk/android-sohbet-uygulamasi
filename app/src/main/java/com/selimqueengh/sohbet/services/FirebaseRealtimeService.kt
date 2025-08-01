package com.selimqueengh.sohbet.services

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue
import com.selimqueengh.sohbet.models.User
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseRealtimeService {
    companion object {
        private const val TAG = "FirebaseRealtimeService"
        private const val USERS_REF = "users"
        private const val FRIEND_REQUESTS_REF = "friend_requests"
    }

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference(USERS_REF)
    private val friendRequestsRef = database.getReference(FRIEND_REQUESTS_REF)

    // 1. Kullanıcı Arama - Kullanıcı adına göre
    suspend fun searchUserByUsername(username: String): Result<User?> {
        return try {
            val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
            
            if (snapshot.exists()) {
                val userData = snapshot.children.first()
                val user = User(
                    id = userData.key ?: "",
                    username = userData.child("username").getValue(String::class.java) ?: "",
                    displayName = userData.child("displayName").getValue(String::class.java) ?: "",
                    avatarUrl = userData.child("avatarUrl").getValue(String::class.java) ?: "",
                    status = com.selimqueengh.sohbet.models.UserStatus.ONLINE,
                    isOnline = userData.child("isOnline").getValue(Boolean::class.java) ?: false,
                    lastSeen = userData.child("lastSeen").getValue(Long::class.java) ?: 0L
                )
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user by username", e)
            Result.failure(e)
        }
    }

    // 2. Arkadaşlık İsteği Gönderme
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            val requestData = hashMapOf(
                "timestamp" to ServerValue.TIMESTAMP,
                "status" to "pending"
            )
            
            friendRequestsRef.child(toUserId).child(fromUserId).setValue(requestData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }

    // 3. Gelen İstekleri Getirme
    suspend fun getIncomingFriendRequests(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = friendRequestsRef.child(userId).get().await()
            val requests = mutableListOf<Map<String, Any>>()
            
            if (snapshot.exists()) {
                for (senderSnapshot in snapshot.children) {
                    val senderId = senderSnapshot.key ?: continue
                    val timestamp = senderSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val status = senderSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    
                    if (status == "pending") {
                        requests.add(mapOf(
                            "senderId" to senderId,
                            "timestamp" to timestamp,
                            "status" to status
                        ))
                    }
                }
            }
            
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting incoming friend requests", e)
            Result.failure(e)
        }
    }

    // 4. İstek Kabul Etme
    suspend fun acceptFriendRequest(receiverId: String, senderId: String): Result<Unit> {
        return try {
            // İsteği kabul et
            friendRequestsRef.child(receiverId).child(senderId).child("status").setValue("accepted").await()
            
            // Her iki kullanıcının friends listesine ekle
            usersRef.child(receiverId).child("friends").child(senderId).setValue(true).await()
            usersRef.child(senderId).child("friends").child(receiverId).setValue(true).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    // 5. İstek Reddetme
    suspend fun rejectFriendRequest(receiverId: String, senderId: String): Result<Unit> {
        return try {
            // İsteği sil
            friendRequestsRef.child(receiverId).child(senderId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            Result.failure(e)
        }
    }

    // 6. Friends Listesini Getirme
    suspend fun getFriends(userId: String): Result<List<User>> {
        return try {
            val snapshot = usersRef.child(userId).child("friends").get().await()
            val friends = mutableListOf<User>()
            
            if (snapshot.exists()) {
                for (friendSnapshot in snapshot.children) {
                    val friendId = friendSnapshot.key ?: continue
                    val isFriend = friendSnapshot.getValue(Boolean::class.java) ?: false
                    
                    if (isFriend) {
                        // Friend'in kullanıcı bilgilerini al
                        val friendUserSnapshot = usersRef.child(friendId).get().await()
                        if (friendUserSnapshot.exists()) {
                            val friendUser = User(
                                id = friendId,
                                username = friendUserSnapshot.child("username").getValue(String::class.java) ?: "",
                                displayName = friendUserSnapshot.child("displayName").getValue(String::class.java) ?: "",
                                avatarUrl = friendUserSnapshot.child("avatarUrl").getValue(String::class.java) ?: "",
                                status = com.selimqueengh.sohbet.models.UserStatus.ONLINE,
                                isOnline = friendUserSnapshot.child("isOnline").getValue(Boolean::class.java) ?: false,
                                lastSeen = friendUserSnapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                            )
                            friends.add(friendUser)
                        }
                    }
                }
            }
            
            Result.success(friends)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friends", e)
            Result.failure(e)
        }
    }

    // 7. Kullanıcı Oluşturma (Login sırasında)
    suspend fun createUser(userId: String, username: String, displayName: String): Result<Unit> {
        return try {
            val userData = hashMapOf(
                "username" to username,
                "displayName" to displayName,
                "avatarUrl" to "",
                "isOnline" to true,
                "lastSeen" to ServerValue.TIMESTAMP,
                "friends" to hashMapOf<String, Boolean>()
            )
            
            usersRef.child(userId).setValue(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(e)
        }
    }

    // 8. Kullanıcı Durumu Güncelleme
    suspend fun updateUserStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            
            usersRef.child(userId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user status", e)
            Result.failure(e)
        }
    }

    // 9. Real-time İstek Dinleme
    fun listenToFriendRequests(userId: String, onRequestAdded: (String, Long) -> Unit, onRequestRemoved: (String) -> Unit) {
        friendRequestsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (senderSnapshot in snapshot.children) {
                        val senderId = senderSnapshot.key ?: continue
                        val status = senderSnapshot.child("status").getValue(String::class.java) ?: "pending"
                        val timestamp = senderSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        
                        if (status == "pending") {
                            onRequestAdded(senderId, timestamp)
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to friend requests", error.toException())
            }
        })
    }

    // 10. Real-time Friends Dinleme
    fun listenToFriends(userId: String, onFriendAdded: (User) -> Unit, onFriendRemoved: (String) -> Unit) {
        usersRef.child(userId).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (friendSnapshot in snapshot.children) {
                        val friendId = friendSnapshot.key ?: continue
                        val isFriend = friendSnapshot.getValue(Boolean::class.java) ?: false
                        
                        if (isFriend) {
                            // Friend bilgilerini al
                            usersRef.child(friendId).get().addOnSuccessListener { friendUserSnapshot ->
                                if (friendUserSnapshot.exists()) {
                                    val friendUser = User(
                                        id = friendId,
                                        username = friendUserSnapshot.child("username").getValue(String::class.java) ?: "",
                                        displayName = friendUserSnapshot.child("displayName").getValue(String::class.java) ?: "",
                                        avatarUrl = friendUserSnapshot.child("avatarUrl").getValue(String::class.java) ?: "",
                                        status = com.selimqueengh.sohbet.models.UserStatus.ONLINE,
                                        isOnline = friendUserSnapshot.child("isOnline").getValue(Boolean::class.java) ?: false,
                                        lastSeen = friendUserSnapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                                    )
                                    onFriendAdded(friendUser)
                                }
                            }
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to friends", error.toException())
            }
        })
    }
}