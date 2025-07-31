package com.selimqueengh.sohbet

import android.app.Application
import com.google.firebase.FirebaseApp

class ChatApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}