package com.example.myapplication.chats

// ✅ IMPORTANT: Do NOT create manual getId() etc.
// Kotlin already creates getters for properties (id, lastMessage, lastTimestamp)

data class ChatList(
    var id: String = "",
    var lastMessage: String = "",
    var lastTimestamp: Long = 0L
)
