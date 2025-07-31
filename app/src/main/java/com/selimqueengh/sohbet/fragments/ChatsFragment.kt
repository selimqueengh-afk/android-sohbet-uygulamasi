package com.selimqueengh.sohbet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.selimqueengh.sohbet.ChatActivity
import com.selimqueengh.sohbet.ChatAdapter
import com.selimqueengh.sohbet.ChatItem
import com.selimqueengh.sohbet.FriendsActivity
import com.selimqueengh.sohbet.R

class ChatsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadSampleChats()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewChats)
        fabNewChat = view.findViewById(R.id.fabNewChat)
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

    private fun setupClickListeners() {
        fabNewChat.setOnClickListener {
            val intent = Intent(requireContext(), FriendsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSampleChats() {
        // Demo chats removed - will load from Firebase
        chatAdapter.notifyDataSetChanged()
    }
}
