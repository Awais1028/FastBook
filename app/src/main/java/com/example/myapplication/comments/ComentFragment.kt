package com.example.myapplication.comments

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
import com.example.myapplication.comments.Comment
import com.example.myapplication.comments.CommentAdapter
import com.example.myapplication.notifications.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentFragment : Fragment() {

    private var postId: String? = null
    private var postOwnerId: String? = null
    private var postCategory: String? = null // 👇 NEW: To store category

    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        // Get the IDs & Category passed from the FeedAdapter
        postId = arguments?.getString("postId")
        postOwnerId = arguments?.getString("postOwnerId")
        postCategory = arguments?.getString("category") // 👇 Grab the category

        val toolbar: Toolbar = view.findViewById(R.id.comment_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Comments"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recycler_view_comments)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 👇 UI FIX: Prevent "Eaten Bottom" behind nav bar
        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, 0, 0, 250) // Adds padding at bottom

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

            if (postOwnerId != null) {
                addNotification(postOwnerId!!, currentUser.uid, notificationText, postId!!)
            } else {
                Log.e("CommentFragment", "Error: postOwnerId is null. Cannot send notification.")
            }

            // 👇 NEW: Add +3 Interest Points for Commenting!
            if (postCategory != null) {
                updateUserInterest(currentUser.uid, postCategory!!, 3)
            }
        }
    }

    // 👇 NEW: Helper function to update scores (Same as in FeedAdapter)
    private fun updateUserInterest(userId: String, category: String, points: Int) {
        if (category == "General" || category.isEmpty()) return

        val interestRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(userId)
            .child("interests")
            .child(category)

        interestRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentScore = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentScore + points
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    private fun addNotification(postOwnerId: String, actorId: String, text: String, postId: String) {
        if (postOwnerId == actorId) {
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

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
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