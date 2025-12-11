package com.example.myapplication.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class FeedItem(
    val postId: String = "",
    val publisher: String = "",
    val userName: String = "",
    val postText: String = "",
    val postImageUrl: String? = null,
    val postVideoUrl: String? = null,
    val timestamp: Long = 0L,
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0,
    val likes: HashMap<String, Boolean> = HashMap(),
    val commentCount: Int = 0,

    // 👇 NEW: Category for Behavioral Score (Default is "General")
    val category: String = "General"
)

// Helper function isn't strictly needed if you save directly in Fragment,
// but if you use it elsewhere, update it too:
fun createPost(text: String, imageUrl: String?, videoUrl: String?, width: Int, height: Int, category: String = "General") {
    val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
    val publisherId = firebaseUser.uid
    val userName = firebaseUser.displayName ?: "User"
    val postsRef = FirebaseDatabase.getInstance().getReference("posts")
    val postId = postsRef.push().key ?: return

    val post = FeedItem(
        postId = postId,
        publisher = publisherId,
        userName = userName,
        postText = text,
        postImageUrl = imageUrl,
        postVideoUrl = videoUrl,
        timestamp = System.currentTimeMillis(),
        mediaWidth = width,
        mediaHeight = height,
        category = category // Save it here
    )

    postsRef.child(postId).setValue(post)
}