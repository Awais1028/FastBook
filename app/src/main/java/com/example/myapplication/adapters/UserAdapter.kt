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
import com.example.myapplication.chats.Chat
import com.example.myapplication.chats.MessageChatActivity // You will need to create this activity
import com.example.myapplication.users.Users
import de.hdodenhof.circleimageview.CircleImageView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

class UserAdapter(
    private val mContext: Context,
    private val mUsers: List<Users>,
    private val isChatCheck: Boolean,
    private val lastMessageMap: Map<String, String> = emptyMap()
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var timeText: TextView = itemView.findViewById(R.id.time_text)
        var unreadBadge: TextView = itemView.findViewById(R.id.unread_badge)

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
    private fun loadLastMessage(otherUserId: String, holder: ViewHolder) {
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("Chats")

        ref.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var lastMsg: Chat? = null

                for (s in snapshot.children) {
                    val chat = s.getValue(Chat::class.java) ?: continue
                    val betweenUs =
                        (chat.sender == myId && chat.receiver == otherUserId) ||
                                (chat.sender == otherUserId && chat.receiver == myId)

                    if (betweenUs) lastMsg = chat
                }

                holder.lastMessageText.text = lastMsg?.message ?: ""
                holder.lastMessageText.visibility = if (holder.lastMessageText.text.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = mUsers[position]
        holder.usernameText.text = user.getFullName()
        holder.timeText.visibility = View.GONE
        holder.unreadBadge.visibility = View.GONE

        if (user.getProfileImageUrl().isNotEmpty()) {
            Glide.with(mContext).load(user.getProfileImageUrl()).into(holder.profileImageView)
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_profile)
        }
        if (isChatCheck) {
            val last = lastMessageMap[user.getUID()] ?: ""
            holder.lastMessageText.text = last
            holder.lastMessageText.visibility = if (last.isEmpty()) View.GONE else View.VISIBLE
        } else {
            holder.lastMessageText.visibility = View.GONE
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