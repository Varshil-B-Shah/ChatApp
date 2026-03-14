package com.example.chatapp.adaptor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.BuildConfig
import com.example.chatapp.R
import com.example.chatapp.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularimageview.CircularImageView

class MessagesAdaptor(
    private val context: Context,
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val orderedMessages: MutableList<ChatMessage> = mutableListOf()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

        private const val APPWRITE_ENDPOINT = BuildConfig.APPWRITE_ENDPOINT
        private const val BUCKET_ID = BuildConfig.APPWRITE_BUCKET_ID
        private const val PROJECT_ID = BuildConfig.APPWRITE_PROJECT_ID

        fun buildProfileImageUrl(imageId: String): String {
            return "$APPWRITE_ENDPOINT/storage/buckets/$BUCKET_ID/files/$imageId/view?project=$PROJECT_ID"
        }
    }

//    init {
//        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                val newMessage = messages.lastOrNull() ?: return
//
//                // Avoid duplicates
//                if (orderedMessages.any {
//                        it.message == newMessage.message &&
//                                it.sender.id == newMessage.sender.id &&
//                                it.timestamp == newMessage.timestamp
//                    }) return
//
//                // Insert at correct chronological position
//                val insertPos = if (newMessage.timestamp == null) {
//                    orderedMessages.size
//                } else {
//                    orderedMessages.indexOfFirst { existing ->
//                        existing.timestamp != null && existing.timestamp.after(newMessage.timestamp)
//                    }.takeIf { it >= 0 } ?: orderedMessages.size
//                }
//
//                orderedMessages.add(insertPos, newMessage)
//
//                Handler(Looper.getMainLooper()).post {
//                    notifyDataSetChanged()
//                }
//            }
//        })
//    }

    fun addMessage(newMessage: ChatMessage) {
        if (orderedMessages.any {
                it.message == newMessage.message &&
                        it.sender.id == newMessage.sender.id &&
                        it.timestamp == newMessage.timestamp
            }) return

        val insertPos = if (newMessage.timestamp == null) {
            orderedMessages.size
        } else {
            orderedMessages.indexOfFirst { existing ->
                existing.timestamp != null && existing.timestamp.after(newMessage.timestamp)
            }.takeIf { it >= 0 } ?: orderedMessages.size
        }

        orderedMessages.add(insertPos, newMessage)
        notifyItemInserted(insertPos)
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        val messageImage: ImageView = itemView.findViewById(R.id.imageViewMessageImage)
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircularImageView = itemView.findViewById(R.id.imageViewProfile)
        val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        val messageImage: ImageView = itemView.findViewById<ImageView>(R.id.imageViewMessageImage)
    }

    override fun getItemViewType(position: Int): Int {
        val message = orderedMessages[position]
        return if (message.sender.id == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = orderedMessages[position]

        // Helper to load message image — reuses the same buildProfileImageUrl since
        // chat images are stored in the same Appwrite bucket
        fun loadMessageImage(imageView: ImageView, imageId: String?) {
            if (!imageId.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                Glide.with(context)
                    .load(buildProfileImageUrl(imageId))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }

        when (holder) {
            is SentViewHolder -> {
                holder.messageText.text = message.message
                loadMessageImage(holder.messageImage, message.imageId)
            }

            is ReceivedViewHolder -> {
                holder.messageText.text = message.message
                loadMessageImage(holder.messageImage, message.imageId)
                // existing profile image logic stays the same...
                val imageId = message.sender.profileImage
                if (imageId.isNotEmpty()) {
                    Glide.with(context)
                        .load(buildProfileImageUrl(imageId))
                        .placeholder(R.drawable.ic_profile)
                        .into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
        }
    }

    override fun getItemCount(): Int = orderedMessages.size
}