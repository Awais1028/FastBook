package com.example.myapplication.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.home.PostDetailFragment
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView // Import CircleImageView

class NotificationAdapter(
    private val notifications: List<NotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image_notification) // ✅ ADDED
        val titleText: TextView = itemView.findViewById(R.id.notificationTitle)
        val messageText: TextView = itemView.findViewById(R.id.notificationMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        holder.messageText.text = item.text


        // Fetch user info (name and profile picture) of the person who acted
        FirebaseDatabase.getInstance().getReference("Users").child(item.actorId).get()
            .addOnSuccessListener { dataSnapshot ->
                val username = dataSnapshot.child("fullName").getValue(String::class.java) ?: "Someone"
                val imageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java) // ✅ GET IMAGE URL

                holder.titleText.text = username

                // ✅ LOAD THE IMAGE
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context).load(imageUrl).into(holder.profileImage)
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_profile) // Fallback placeholder
                }
            }.addOnFailureListener {
                holder.titleText.text = "Notification"
                holder.profileImage.setImageResource(R.drawable.ic_profile) // Fallback
            }
        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val clicked = notifications[currentPosition]
            val activity = holder.itemView.context as? com.example.myapplication.home.FeedActivity
            activity?.openPostDetail(clicked.postId)
        }

    }

    override fun getItemCount(): Int = notifications.size
}