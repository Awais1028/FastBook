package com.example.myapplication.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.chats.MessageChatActivity // You will need to create this activity
import com.example.myapplication.users.Users
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val mContext: Context,
    private val mUsers: List<Users>,
    private val isChatCheck: Boolean
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var usernameText: TextView = itemView.findViewById(R.id.username) // Assumes you have a user_item.xml
        var profileImageView: CircleImageView = itemView.findViewById(R.id.profile_image)
        var onlineStatus: CircleImageView = itemView.findViewById(R.id.image_online)
        var offlineStatus: CircleImageView = itemView.findViewById(R.id.image_offline)
        var lastMessageText: TextView = itemView.findViewById(R.id.message_last)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // You need a layout file named 'user_search_item_layout.xml' for this to work
        val view = LayoutInflater.from(mContext).inflate(R.layout.user_search_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mUsers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = mUsers[position]
        holder.usernameText.text = user.getFullName()

        if (user.getProfileImageUrl().isNotEmpty()) {
            Glide.with(mContext).load(user.getProfileImageUrl()).into(holder.profileImageView)
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_profile)
        }

        // Handle click to open a chat screen
        holder.itemView.setOnClickListener {
            val intent = Intent(mContext, MessageChatActivity::class.java)
            intent.putExtra("visit_id", user.getUID())
            mContext.startActivity(intent)
        }

        // Logic for online/offline status (can be implemented later)
        if (isChatCheck) {
            holder.lastMessageText.visibility = View.VISIBLE
        } else {
            holder.lastMessageText.visibility = View.GONE
        }
    }
}