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
import com.example.myapplication.chats.MessageChatActivity
import com.example.myapplication.users.Users
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val mContext: Context,
    private val mUsers: List<Users>,
    private val isChatCheck: Boolean,
    private val lastMessageMap: Map<String, String> = emptyMap(),
    private val lastTimeMap: Map<String, Long> = emptyMap()
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var usernameText: TextView = itemView.findViewById(R.id.username)
        var profileImageView: CircleImageView = itemView.findViewById(R.id.profile_image)
        var lastMessageText: TextView = itemView.findViewById(R.id.message_last)

        // optional views if exist in XML (safe if you remove these from xml)
        var timeText: TextView? = itemView.findViewById(R.id.time_text)
        var unreadBadge: TextView? = itemView.findViewById(R.id.unread_badge)
        var onlineStatus: CircleImageView? = itemView.findViewById(R.id.image_online)
        var offlineStatus: CircleImageView? = itemView.findViewById(R.id.image_offline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.user_search_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mUsers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = mUsers[position]

        holder.usernameText.text = user.getFullName()

        val img = user.getProfileImageUrl()
        if (img.isNotEmpty()) {
            Glide.with(mContext).load(img).into(holder.profileImageView)
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_profile)
        }

        if (isChatCheck) {
            // ✅ Last message
            val lastMsg = lastMessageMap[user.getUID()] ?: ""
            holder.lastMessageText.text = lastMsg
            holder.lastMessageText.visibility =
                if (lastMsg.isBlank()) View.GONE else View.VISIBLE

            // ✅ Last time
            val ts = lastTimeMap[user.getUID()]
            if (ts != null) {
                holder.timeText?.text = formatTime(ts)
                holder.timeText?.visibility = View.VISIBLE
            } else {
                holder.timeText?.visibility = View.GONE
            }
        } else {
            holder.lastMessageText.visibility = View.GONE
            holder.timeText?.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(mContext, MessageChatActivity::class.java)
            intent.putExtra("visit_id", user.getUID())
            mContext.startActivity(intent)
        }
    }
    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val oneDay = 24 * 60 * 60 * 1000

        val sdfTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val sdfDate = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())

        return when {
            diff < oneDay -> sdfTime.format(java.util.Date(timestamp))
            diff < 2 * oneDay -> "Yesterday"
            else -> sdfDate.format(java.util.Date(timestamp))
        }
    }

}
