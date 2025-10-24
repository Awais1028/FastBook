package com.example.myapplication.chats

data class ChatList(
    private var id: String = ""
) {
    fun getId(): String {
        return id
    }
}