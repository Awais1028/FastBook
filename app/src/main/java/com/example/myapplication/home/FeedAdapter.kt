package com.example.myapplication.home
// (Assuming your FeedAdapter is in the Adapter package based on your project structure)

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FeedAdapter(private val feedList: MutableList<FeedItem>) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {
// In FeedAdapter.kt

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Find all the views from your new layout
        val userNameText: TextView = itemView.findViewById(R.id.textUserName)
        val timeStampText: TextView = itemView.findViewById(R.id.textTimestamp)
        val postText: TextView = itemView.findViewById(R.id.textPost)
        val postImage: ImageView = itemView.findViewById(R.id.imagePost)
        val postVideo: VideoView = itemView.findViewById(R.id.videoPost)

        // Add references for your new like/comment icons
        val likeIcon: ImageView = itemView.findViewById(R.id.iconLike)
        val commentIcon: ImageView = itemView.findViewById(R.id.iconComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    // --- UPDATED: Format Long timestamp to "time ago" ---
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
            else -> {
                // If older than a week, show actual date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = feedList[position]

        holder.userNameText.text = item.userName
        // --- UPDATED: Call with Long timestamp ---
        holder.timeStampText.text = formatTimeAgo(item.timestamp)
        holder.postText.text = item.postText

        // Reset click listeners to avoid recycled callbacks
        holder.postImage.setOnClickListener(null)
        holder.postVideo.setOnPreparedListener(null)
        holder.postText.setOnClickListener(null) // Reset for text, in case it was set for video link fallback

        // Hide both media views by default to ensure clean state for recycling
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE

        holder.likeIcon.setOnClickListener {
            // Get the specific post that was clicked
            val clickedPost = feedList[position]

            // TODO: Add logic here to update the like count in Firebase
            Toast.makeText(holder.itemView.context, "Liked post by " + clickedPost.userName, Toast.LENGTH_SHORT).show()
        }

        // 1) If there is an explicit image URL -> show it
        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .centerCrop()
                .into(holder.postImage)
            return // Stop here, we found media
        }

        // 2) If there is a video URL
        if (!item.postVideoUrl.isNullOrEmpty()) {
            val videoUrl = item.postVideoUrl

            // If it's a YouTube link → show YouTube thumbnail in postImage and open external player on click
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

                if (!thumbUrl.isNullOrEmpty()) {
                    holder.postImage.visibility = View.VISIBLE // Use image view for thumbnail
                    Glide.with(holder.itemView.context).load(thumbUrl).centerCrop().into(holder.postImage)

                    holder.postImage.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        holder.itemView.context.startActivity(intent)
                    }
                    return // Stop here, we found media
                }
            }

            // For direct video links (http(s) mp4) or android.resource URI -> play inside VideoView
            holder.postVideo.visibility = View.VISIBLE // Use video view for direct video

            try {
                holder.postVideo.setVideoURI(Uri.parse(videoUrl))
                holder.postVideo.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    holder.postVideo.start()
                }
            } catch (e: Exception) {
                // fallback: hide video view and show link as clickable text
                holder.postVideo.visibility = View.GONE
                holder.postText.append("\n\n▶ Video: $videoUrl") // Append to existing text
                holder.postText.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                    holder.itemView.context.startActivity(intent)
                }
            }
            return // Stop here, we found media
        }

        // 3) No media -> both are already GONE due to initial reset or previous logic.
        // No explicit action needed here for text-only, as the textPost will always be visible.
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