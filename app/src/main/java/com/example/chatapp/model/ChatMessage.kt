package com.example.chatapp.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessage(
    val sender: User = User(),
    val message: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)