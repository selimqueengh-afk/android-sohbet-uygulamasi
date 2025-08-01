package com.selimqueengh.sohbet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.ChatActivity
import com.selimqueengh.sohbet.ChatAdapter
import com.selimqueengh.sohbet.R
import com.selimqueengh.sohbet.models.ChatItem

class ChatsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        loadChats()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewChats)
        emptyStateText = view.findViewById(R.id.emptyStateText)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList) { chatItem ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chat_partner", chatItem.username)
            intent.putExtra("chat_id", chatItem.chatId)
            startActivity(intent)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun loadChats() {
        // Load chats from Firebase
        val sharedPreferences = requireActivity().getSharedPreferences("SnickersChatv4", android.content.Context.MODE_PRIVATE)
        val currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        if (currentUserId.isNotEmpty()) {
            val firebaseService = com.selimqueengh.sohbet.services.FirebaseService()
            
            androidx.lifecycle.lifecycleScope.launch {
                try {
                    val result = firebaseService.getChats(currentUserId)
                    if (result.isSuccess) {
                        val userChats = result.getOrNull() ?: emptyList()
                        chatList.clear()
                        
                        for (chatData in userChats) {
                            val participants = chatData["participants"] as? List<String> ?: emptyList()
                            val otherUserId = participants.find { it != currentUserId } ?: continue
                            
                            // Get other user info
                            val otherUserResult = firebaseService.getUserById(otherUserId)
                            val otherUser = otherUserResult.getOrNull()
                            
                            val chatItem = com.selimqueengh.sohbet.models.ChatItem(
                                chatId = chatData["chatId"] as? String ?: "",
                                username = otherUser?.username ?: "Bilinmeyen Kullanıcı",
                                lastMessage = chatData["lastMessage"] as? String ?: "Henüz mesaj yok",
                                timestamp = (chatData["lastMessageTimestamp"] as? java.util.Date)?.time ?: System.currentTimeMillis(),
                                unreadCount = 0,
                                isOnline = otherUser?.isOnline ?: false
                            )
                            chatList.add(chatItem)
                        }
                        
                        requireActivity().runOnUiThread {
                            if (chatList.isEmpty()) {
                                emptyStateText.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                            } else {
                                emptyStateText.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                            }
                            chatAdapter.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        emptyStateText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                }
            }
        } else {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }
}
