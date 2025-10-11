package com.example.myapplication.home

import FeedItem
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedAdapter(private val feedList: MutableList<FeedItem>) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameText: TextView = itemView.findViewById(R.id.textUserName)
        val timeStampText: TextView = itemView.findViewById(R.id.textTimestamp)
        val postText: TextView = itemView.findViewById(R.id.textPost)
        val postImage: ImageView = itemView.findViewById(R.id.imagePost)
        val postVideo: VideoView = itemView.findViewById(R.id.videoPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    // New function to format ISO 8601 string to "time ago"
    private fun formatTimeAgo(isoTimestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = parser.parse(isoTimestamp)

            val now = System.currentTimeMillis()
            val diff = now - (date?.time ?: now)

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours h ago"
                else -> "$days d ago"
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = feedList[position]

        holder.userNameText.text = item.userName
        // Now calling the new function with the String timestamp
        holder.timeStampText.text = formatTimeAgo(item.timeStamp)
        holder.postText.text = item.postText

        // Reset click listeners to avoid recycled callbacks
        holder.postImage.setOnClickListener(null)
        holder.postVideo.setOnPreparedListener(null)

        // 1) If there is an explicit image URL -> show it
        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            holder.postVideo.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .centerCrop()
                .into(holder.postImage)
            return
        } else {
            holder.postImage.visibility = View.GONE
        }

        // 2) If there is a video URL
        if (!item.postVideoUrl.isNullOrEmpty()) {
            val videoUrl = item.postVideoUrl

            // If it's a YouTube link → show YouTube thumbnail in postImage and open external player on click
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                // extract video id
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

                if (!thumbUrl.isNullOrEmpty()) {
                    holder.postImage.visibility = View.VISIBLE
                    holder.postVideo.visibility = View.GONE
                    Glide.with(holder.itemView.context).load(thumbUrl).centerCrop().into(holder.postImage)

                    // clicking thumbnail opens YouTube app/browser
                    holder.postImage.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        holder.itemView.context.startActivity(intent)
                    }
                    return
                }
            }

            // For direct video links (http(s) mp4) or android.resource URI -> play inside VideoView
            holder.postImage.visibility = View.GONE
            holder.postVideo.visibility = View.VISIBLE

            try {
                holder.postVideo.setVideoURI(Uri.parse(videoUrl))
                holder.postVideo.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    holder.postVideo.start()
                }
            } catch (e: Exception) {
                // fallback: hide video view and show link as clickable text
                holder.postVideo.visibility = View.GONE
                holder.postText.append("\n\n▶ Video: $videoUrl")
                holder.postText.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                    holder.itemView.context.startActivity(intent)
                }
            }
            return
        }

        // 3) No media -> hide both
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE
    }

    override fun getItemCount(): Int = feedList.size

    // helper to extract youtube video id
    private fun extractYoutubeId(url: String): String? {
        // supports "v=" or youtu.be short links
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> null
        }
    }
}