package com.example.myapplication.comments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.notifications.NotificationItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentFragment : BottomSheetDialogFragment() {

    private var postId: String? = null
    private var postOwnerId: String? = null
    private var postCategory: String? = null
    // 👇 NEW: Variable to track count locally
    private var currentCommentCount: Int = 0

    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    private lateinit var addCommentEditText: EditText
    private lateinit var postCommentButton: TextView

    // --- 1. CRITICAL STRUCTURAL FIX: RETRIEVE ARGUMENTS IN ONCREATE ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments immediately upon Fragment creation
        postId = arguments?.getString("postId")
        postOwnerId = arguments?.getString("postOwnerId")
        postCategory = arguments?.getString("category")

        // 👇 NEW: Get the starting count passed from FeedAdapter
        currentCommentCount = arguments?.getInt("commentCount", 0) ?: 0

        Log.d("CommentDebug", "onCreate: Retrieved Post ID: $postId, Start Count: $currentCommentCount")
    }

    // --- 2. SET UP BOTTOM SHEET BEHAVIOR ---
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener {
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    // --- 3. ONLY INFLATE VIEW ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment, container, false)
    }

    // --- 4. INITIALIZE LOGIC AND UI ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Final check on arguments retrieved in onCreate
        if (postId.isNullOrEmpty()) {
            Toast.makeText(context, "FATAL ERROR: Post ID is missing. Please restart.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }
        // --- UI Setup ---
        recyclerView = view.findViewById(R.id.recycler_view_comments)
        recyclerView.layoutManager = LinearLayoutManager(context)
        commentAdapter = CommentAdapter(commentList)
        recyclerView.adapter = commentAdapter

        addCommentEditText = view.findViewById(R.id.add_comment_edittext)
        postCommentButton = view.findViewById(R.id.post_comment_button)

        // --- Listeners ---
        postCommentButton.setOnClickListener {
            if (addCommentEditText.text.toString().isNotEmpty()) {
                postComment(addCommentEditText.text.toString())
                addCommentEditText.text.clear()
            } else {
                Toast.makeText(context, "Empty comment!", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Data Load ---
        loadComments()
        Log.d("CommentDebug", "Initialization complete. Loading comments for $postId")
    }

    // --- Data Logic Functions (Using safe calls) ---

    private fun postComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Safety check using the ID field directly
        if (postId.isNullOrEmpty()) {
            Toast.makeText(context, "Cannot post: ID error.", Toast.LENGTH_SHORT).show()
            return
        }

        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postId!!) // Relying on non-null check from caller
        val commentId = commentsRef.push().key!!

        val comment = Comment(
            commentId = commentId,
            comment = commentText,
            publisher = currentUser.uid,
            timestamp = System.currentTimeMillis()
        )
        commentsRef.child(commentId).setValue(comment).addOnSuccessListener {
            // 1. Update Database Count
            incrementCommentCount()

            // 2. 👇 NEW: Update Local Count & Send to Feed
            currentCommentCount++

            val result = Bundle().apply {
                putString("updatedPostId", postId)
                putInt("newCommentCount", currentCommentCount)
            }
            // This sends the signal back to FeedFragment instantly
            parentFragmentManager.setFragmentResult("post_update", result)


            val notificationText = "commented: $commentText"

            if (postOwnerId != null) {
                addNotification(postOwnerId!!, currentUser.uid, notificationText, postId!!)
            }
            if (postCategory != null) {
                updateUserInterest(currentUser.uid, postCategory!!, 3)
            }
        }
    }

    private fun loadComments() {
        val currentPostId = postId // No '!!' here, we'll check it below

        // --- 1. DUMMY DATA INJECTION REMOVED FOR CLEANER PROD CODE ---

        if (currentPostId.isNullOrEmpty()) {
            Log.e("CommentDebug", "Cannot attach Firebase listener: Post ID is null.")
            return
        }

        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postId!!)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    commentList.clear() // Clear dummy and old data
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        comment?.let { commentList.add(it) }
                    }
                    commentAdapter.notifyDataSetChanged()

                    if (commentList.isNotEmpty()) {
                        recyclerView.post {
                            recyclerView.scrollToPosition(commentList.size - 1)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CommentDebug", "FATAL CRASH DURING DATA PARSING (CommentFragment): ${e.message}", e)
                    Toast.makeText(context, "Data parsing failed: See log.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CommentDebug", "Firebase LOAD Failed: ${error.message}")
                Toast.makeText(context, "Failed to load comments: Check network.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- Keep existing helper functions ---
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
        if (postOwnerId == actorId) return
        val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(postOwnerId)
        val notificationId = notifRef.push().key ?: return
        val notification = NotificationItem(notificationId, actorId, text, postId, System.currentTimeMillis())
        notifRef.child(notificationId).setValue(notification)
    }

    private fun incrementCommentCount() {
        val currentPostId = postId ?: return
        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(currentPostId)
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
                    Log.e("CommentDebug", "Failed to increment comment count: ${error.message}")
                }
            }
        })
    }


}