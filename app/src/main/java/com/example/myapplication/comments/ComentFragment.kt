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
        Log.d("CommentDebug", "onCreate: Retrieved Post ID: $postId")
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
            incrementCommentCount()
            val notificationText = "commented: $commentText"

            if (postOwnerId != null) {
                addNotification(postOwnerId!!, currentUser.uid, notificationText, postId!!)
            }
            if (postCategory != null) {
                updateUserInterest(currentUser.uid, postCategory!!, 3)
            }
        }
    }

    // 2. Original Load Comments (Working Syntax Restored)
    // In CommentFragment.kt

    private fun loadComments() {
        val currentPostId = postId // No '!!' here, we'll check it below

        // --- 1. DUMMY DATA INJECTION (FOR TESTING) ---
        // If the RecyclerView displays these three, the layout and adapter are working.
        commentList.add(Comment(commentId = "dummy1", comment = "Dummy Comment 1 (Test Load)", publisher = "TestUser1", timestamp = System.currentTimeMillis() - 120000))
        commentList.add(Comment(commentId = "dummy2", comment = "Dummy Comment 2 (Test Load)", publisher = "TestUser2", timestamp = System.currentTimeMillis() - 60000))
        commentList.add(Comment(commentId = "dummy3", comment = "Dummy Comment 3 (Test Load)", publisher = "TestUser3", timestamp = System.currentTimeMillis()))
        commentAdapter.notifyDataSetChanged()
        Log.d("CommentDebug", "Dummy data injected. Adapter notified.")

        if (currentPostId.isNullOrEmpty()) {
            Log.e("CommentDebug", "Cannot attach Firebase listener: Post ID is null.")
            return
        }

        // --- 2. ACTUAL FIREBASE LISTENER ATTACHMENT ---
        // We clear the list inside the listener, so the dummy data will disappear
        // when the real data arrives, which is normal.

        // Using postId!! here because we checked it above and we rely on the working syntax
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postId!!)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // 🔴 CRITICAL: The error is in the parsing or binding inside this block.
                try {
                    commentList.clear() // Clear dummy and old data
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        comment?.let { commentList.add(it) }
                    }
                    commentAdapter.notifyDataSetChanged()
                    Log.d("CommentDebug", "Firebase LOAD Success: Added ${commentList.size} real comments.")

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
    private fun updateUserInterest(userId: String, category: String, points: Int) { /* Logic omitted for brevity */ }
    private fun addNotification(postOwnerId: String, actorId: String, text: String, postId: String) { /* Logic omitted for brevity */ }
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