package com.selimqueengh.sohbet.fragments

import android.content.Context
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
import com.selimqueengh.sohbet.FriendsAdapter
import com.selimqueengh.sohbet.R
import com.selimqueengh.sohbet.models.Friend

class FriendsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var addFriendButton: FloatingActionButton
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        addSampleFriends()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewFriends)
        addFriendButton = view.findViewById(R.id.addFriendButton)
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(friendsList) { friend ->
            openChatWithFriend(friend)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }
    }

    private fun setupClickListeners() {
        addFriendButton.setOnClickListener {
            addSampleFriend()
        }
    }

    private fun openChatWithFriend(friend: Friend) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chat_partner", friend.name)
        
        // Mevcut kullanıcı adını al
        val sharedPref = requireContext().getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        val currentUsername = sharedPref.getString("username", "User") ?: "User"
        intent.putExtra("current_username", currentUsername)
        
        startActivity(intent)
    }

    private fun addSampleFriends() {
        val sampleFriends = listOf(
            Friend("Ahmet Yılmaz", "Online", "avatar1"),
            Friend("Ayşe Demir", "Son görülme: 2 saat önce", "avatar2"),
            Friend("Mehmet Kaya", "Online", "avatar3"),
            Friend("Fatma Özkan", "Son görülme: 1 gün önce", "avatar4"),
            Friend("Ali Çelik", "Online", "avatar5")
        )
        friendsList.addAll(sampleFriends)
        friendsAdapter.notifyDataSetChanged()
    }

    private fun addSampleFriend() {
        val newFriend = Friend("Yeni Arkadaş", "Online", "avatar_default")
        friendsList.add(newFriend)
        friendsAdapter.notifyItemInserted(friendsList.size - 1)
    }
}
