package com.selimqueengh.sohbet

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.selimqueengh.sohbet.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messages: List<Message>) : 
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByUser) VIEW_TYPE_USER else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_other
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        private val messageTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val messageImage: ImageView? = itemView.findViewById(R.id.imageViewMessage)

        fun bind(message: Message) {
            when (message.messageType) {
                "image" -> {
                    if (message.mediaData.isNotEmpty()) {
                        messageText.visibility = View.GONE
                        messageImage?.visibility = View.VISIBLE
                        
                        try {
                            val imageBytes = android.util.Base64.decode(message.mediaData, android.util.Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                messageImage?.setImageBitmap(bitmap)
                                // Modern görünüm için corner radius ekle
                                messageImage?.clipToOutline = true
                            } else {
                                messageText.visibility = View.VISIBLE
                                messageImage?.visibility = View.GONE
                                messageText.text = "📷 Resim yüklenemedi"
                            }
                        } catch (e: Exception) {
                            messageText.visibility = View.VISIBLE
                            messageImage?.visibility = View.GONE
                            messageText.text = "📷 Resim yüklenemedi: ${e.message}"
                        }
                    } else {
                        messageText.visibility = View.VISIBLE
                        messageImage?.visibility = View.GONE
                        messageText.text = "📷 Resim"
                    }
                }
                "video" -> {
                    messageText.visibility = View.VISIBLE
                    messageImage?.visibility = View.GONE
                    messageText.text = "🎥 Video"
                }
                else -> {
                    messageText.visibility = View.VISIBLE
                    messageImage?.visibility = View.GONE
                    messageText.text = message.text
                }
            }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            messageTime.text = timeFormat.format(Date(message.timestamp))
        }
    }
}