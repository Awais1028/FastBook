package com.example.myapplication.notifications

data class NotificationItem(
    val notificationId: String = "",
    val actorId: String = "", // The UID of the user who liked/commented
    val text: String = "",    // The message, e.g., "liked your post."
    val postId: String = "",    // The ID of the post that was interacted with
    val timestamp: Long = 0L
)