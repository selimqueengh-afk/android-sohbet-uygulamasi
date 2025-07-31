package com.selimqueengh.sohbet.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.selimqueengh.sohbet.R

class ProfileFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var statusText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadUserData()
    }

    private fun initViews(view: View) {
        usernameText = view.findViewById(R.id.username)
        statusText = view.findViewById(R.id.status)
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "Kullanıcı") ?: "Kullanıcı"
        
        usernameText.text = username
        statusText.text = "Online"
    }
}
