package com.example.myapplication.chats
data class Chat(
    val sender: String = "",
    val receiver: String = "",
    val message: String = "",
    val url: String = "",
    val isseen: Boolean = false,
    val timestamp: Long = 0L
)
