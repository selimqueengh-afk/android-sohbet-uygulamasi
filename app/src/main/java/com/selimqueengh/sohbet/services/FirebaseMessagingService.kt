package com.selimqueengh.sohbet.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.selimqueengh.sohbet.ChatActivity
import com.selimqueengh.sohbet.MainActivity
import com.selimqueengh.sohbet.R

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Chat Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new messages"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "New Message"
            val message = remoteMessage.data["message"] ?: "You have a new message"
            val chatId = remoteMessage.data["chatId"]
            val senderId = remoteMessage.data["senderId"]

            // Show notification
            sendNotification(title, message, chatId, senderId)
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "New Message"
            val message = notification.body ?: "You have a new message"
            val chatId = remoteMessage.data["chatId"]
            val senderId = remoteMessage.data["senderId"]

            sendNotification(title, message, chatId, senderId)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Send token to your server
        // This should be implemented to update the user's FCM token in Firestore
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        chatId: String?,
        senderId: String?
    ) {
        val intent = if (chatId != null) {
            Intent(this, ChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("chat_id", chatId)
                putExtra("sender_id", senderId)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_simple)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}