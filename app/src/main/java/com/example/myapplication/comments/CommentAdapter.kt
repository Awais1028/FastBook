package com.example.myapplication.comments

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class CommentAdapter(private val commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image_comment)
        val username: TextView = itemView.findViewById(R.id.username_comment)
        val comment: TextView = itemView.findViewById(R.id.comment_text)
        val time: TextView = itemView.findViewById(R.id.comment_time) // 👈 ADDED TIME VIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = commentList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("CommentAdapterDebug", "Binding position: $position. Comment: ${commentList[position].comment}")
        val comment = commentList[position]

        holder.comment.text = comment.comment
        holder.time.text = getShortTimeAgo(comment.timestamp) // 👈 Format Time

        // Fetch User Info
        FirebaseDatabase.getInstance().getReference("Users").child(comment.publisher).get()
            .addOnSuccessListener { dataSnapshot ->
                val username = dataSnapshot.child("fullName").getValue(String::class.java) ?: "Anonymous"
                val imageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java)

                holder.username.text = username
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context).load(imageUrl).into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
    }

    // 👇 HELPER: Instagram-style time format
    private fun getShortTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }
}