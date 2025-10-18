package com.example.myapplication.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// Note: SimpleDateFormat, Date, and Locale imports are no longer needed here.

/**
 * ## Updated FeedItem Data Class
 * The `timestamp` is now a Long for proper sorting.
 */
data class FeedItem(
    val postId: String = "",
    val publisher: String = "", // The user's unique ID (uid)
    val userName: String = "",
    val postText: String = "",
    val postImageUrl: String? = null,
    val postVideoUrl: String? = null,
    val timestamp: Long = 0L, // The correct data type for time
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0
)

/**
 * ## Updated createPost Function
 * This function now saves the current time as a Long using System.currentTimeMillis()
 * and populates all the fields of the FeedItem.
 */
fun createPost(text: String, imageUrl: String? = null, videoUrl: String? = null) {
    // 1. Get current user's info
    val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
    val publisherId = firebaseUser.uid
    val userName = firebaseUser.displayName ?: "User"

    // 2. Get a reference to the "posts" node in Firebase
    val postsRef = FirebaseDatabase.getInstance().getReference("posts")

    // 3. Generate a unique ID for the new post
    val postId = postsRef.push().key ?: return

    // 4. Create the post object with all the correct data
    val post = FeedItem(
        postId = postId,
        publisher = publisherId,
        userName = userName,
        postText = text,
        postImageUrl = imageUrl,
        postVideoUrl = videoUrl,
        timestamp = System.currentTimeMillis() // Saves the current time as a Long

    )

    // 5. Save the complete post object to the database
    postsRef.child(postId).setValue(post)
        .addOnSuccessListener {
            // Post was successful
        }
        .addOnFailureListener { e ->
            // Handle the error
        }
}