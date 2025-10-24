package com.example.myapplication.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.profile.ProfileFragment
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView // Import CircleImageView

class CommentAdapter(private val commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image_comment) // ✅ ADDED
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

        // Fetch user info (name and profile picture)
        FirebaseDatabase.getInstance().getReference("Users").child(comment.publisher).get()
            .addOnSuccessListener { dataSnapshot ->
                val username = dataSnapshot.child("fullName").getValue(String::class.java) ?: "Anonymous"
                val imageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java) // ✅ GET IMAGE URL

                holder.username.text = username

                // ✅ LOAD THE IMAGE
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context).load(imageUrl).into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_profile) // Fallback placeholder
                }
            }
            .addOnFailureListener {
                holder.username.text = "Error User"
                holder.profileImage.setImageResource(R.drawable.ic_profile) // Fallback
            }
        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val clickedComment = commentList[currentPosition]

                // Create the ProfileFragment
                val fragment = ProfileFragment()

                // Create a bundle to pass the UID of the commenter
                val args = Bundle()
                args.putString("uid", clickedComment.publisher)
                fragment.arguments = args

                // Perform the fragment transaction to open the profile
                val activity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment) // Use your main fragment container ID
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}