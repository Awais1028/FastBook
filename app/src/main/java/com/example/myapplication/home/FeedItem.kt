import android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// FeedItem.kt (already)
data class FeedItem(
    val userName: String = "",
    val timeStamp: String = "",
    val postText: String = "",
    val postImageUrl: String? = null,
    val postVideoUrl: String? = null
)

// In your activity/fragment where you create posts:
val postsRef = FirebaseDatabase.getInstance().getReference("posts")

fun createPost(text: String, imageUrl: String? = null, videoUrl: String? = null) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "User"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val formattedTimestamp = dateFormat.format(Date())
    val post = FeedItem(
        userName = userName,
        timeStamp = formattedTimestamp,  // <-- LONG timestamp
        postText = text,
        postImageUrl = imageUrl,
        postVideoUrl = videoUrl
    )

    val key = postsRef.push().key ?: return
    postsRef.child(key).setValue(post)
        .addOnSuccessListener { /* notify user */ }
        .addOnFailureListener { e -> /* handle error */ }
}
