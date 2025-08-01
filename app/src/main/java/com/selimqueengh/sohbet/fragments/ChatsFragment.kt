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
        // TODO: Load chats from Firebase
        if (chatList.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }
}
