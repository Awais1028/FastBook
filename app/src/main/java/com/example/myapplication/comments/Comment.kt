package com.example.myapplication.comments // Assuming your Comment data class is here

data class Comment(
    val commentId: String = "",
    val comment: String = "",
    val publisher: String = "", // The UID of the user who commented
    val timestamp: Long = 0L
)