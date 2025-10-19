package com.example.myapplication.comments // Correct package

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.comments.Comment // Correct import for your Comment data class
import com.example.myapplication.comments.CommentAdapter
import com.example.myapplication.notifications.NotificationItem // Correct import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentFragment : Fragment() {

    private var postId: String? = null
    private var postOwnerId: String? = null // Correctly declared here
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        // Get the IDs that were passed from the FeedAdapter
        postId = arguments?.getString("postId")
        postOwnerId = arguments?.getString("postOwnerId") // Correctly initialized here

        val toolbar: Toolbar = view.findViewById(R.id.comment_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Comments"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recycler_view_comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        commentAdapter = CommentAdapter(commentList)
        recyclerView.adapter = commentAdapter

        val addCommentEditText: EditText = view.findViewById(R.id.add_comment_edittext)
        val postCommentButton: TextView = view.findViewById(R.id.post_comment_button)

        postCommentButton.setOnClickListener {
            if (addCommentEditText.text.toString().isNotEmpty()) {
                postComment(addCommentEditText.text.toString())
                addCommentEditText.text.clear()
            } else {
                Toast.makeText(context, "You can't post an empty comment.", Toast.LENGTH_SHORT).show()
            }
        }

        loadComments()

        return view
    }

    private fun postComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postId!!)
        val commentId = commentsRef.push().key!!

        val comment = Comment(
            commentId = commentId,
            comment = commentText,
            publisher = currentUser.uid,
            timestamp = System.currentTimeMillis()
        )
        commentsRef.child(commentId).setValue(comment).addOnSuccessListener {
            incrementCommentCount()
            val notificationText = "commented: $commentText"

            // --- FIX: Safely call addNotification and log if postOwnerId is null ---
            if (postOwnerId != null) {
                addNotification(postOwnerId!!, currentUser.uid, notificationText, postId!!)
            } else {
                Log.e("CommentFragment", "Error: postOwnerId is null. Cannot send notification.")
                Toast.makeText(context, "Could not send notification: Post owner not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This function is now complete and will be called
    private fun addNotification(postOwnerId: String, actorId: String, text: String, postId: String) {
        if (postOwnerId == actorId) {
            Log.d("CommentFragment", "Not sending notification for self-comment.")
            return
        }

        val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(postOwnerId)
        val notificationId = notifRef.push().key ?: return

        val notification = NotificationItem(
            notificationId = notificationId,
            actorId = actorId,
            text = text,
            postId = postId,
            timestamp = System.currentTimeMillis()
        )
        notifRef.child(notificationId).setValue(notification)
            .addOnSuccessListener {
                Log.d("CommentFragment", "Notification sent to $postOwnerId for post $postId")
            }
            .addOnFailureListener { e ->
                Log.e("CommentFragment", "Failed to send notification: ${e.message}", e)
            }
    }

    private fun incrementCommentCount() {
        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId!!)
        postRef.child("commentCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var count = currentData.getValue(Int::class.java)
                if (count == null) {
                    count = 1
                } else {
                    count++
                }
                currentData.value = count
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("CommentFragment", "Failed to increment comment count: ${error.message}")
                }
            }
        })
    }

    private fun loadComments() {
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postId!!)
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    comment?.let { commentList.add(it) }
                }
                commentAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load comments.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}