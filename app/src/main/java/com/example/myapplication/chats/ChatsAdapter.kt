package com.example.myapplication.chats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.chats.Chat
import com.google.firebase.auth.FirebaseAuth

class ChatsAdapter(
    private val mContext: Context,
    private val mChatList: List<Chat>
) : RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

    companion object {
        const val MSG_TYPE_LEFT = 0
        const val MSG_TYPE_RIGHT = 1
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var showMessage: TextView = itemView.findViewById(R.id.show_message)
        var textSeen: TextView = itemView.findViewById(R.id.text_seen)
        var textTime: TextView = itemView.findViewById(R.id.text_time)

    }
    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = if (viewType == MSG_TYPE_RIGHT) {
            LayoutInflater.from(mContext).inflate(R.layout.message_item_right, parent, false)
        } else {
            LayoutInflater.from(mContext).inflate(R.layout.message_item_left, parent, false)
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mChatList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = mChatList[position]
        holder.showMessage.text = chat.message
        holder.showMessage.text = chat.message
        holder.textTime.text = formatTime(chat.timestamp)
        // Logic for "seen" status can be added here
    }

    override fun getItemViewType(position: Int): Int {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return if (mChatList[position].sender == firebaseUser!!.uid) {
            MSG_TYPE_RIGHT
        } else {
            MSG_TYPE_LEFT
        }
    }
}