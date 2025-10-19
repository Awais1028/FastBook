package com.example.myapplication.comments // Correct package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.comments.Comment // Correct import for your Comment data class
import com.google.firebase.database.FirebaseDatabase

class CommentAdapter(private val commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.username_comment)
        val comment: TextView = itemView.findViewById(R.id.comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = commentList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentList[position]
        holder.comment.text = comment.comment

        // --- FIX: Fetch the username dynamically ---
        FirebaseDatabase.getInstance().getReference("Users").child(comment.publisher).get()
            .addOnSuccessListener { dataSnapshot ->
                val username = dataSnapshot.child("fullName").getValue(String::class.java) ?: "Anonymous"
                holder.username.text = username
            }
            .addOnFailureListener {
                holder.username.text = "Error User" // Fallback on error
            }
    }
}