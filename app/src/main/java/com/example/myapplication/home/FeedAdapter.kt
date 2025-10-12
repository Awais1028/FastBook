package com.example.myapplication.home // Correct package name

import com.example.myapplication.home.FeedItem // Correct path to your FeedItem model
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
import androidx.core.net.toUri

class FeedAdapter(private val feedList: MutableList<FeedItem>) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameText: TextView = itemView.findViewById(R.id.textUserName)
        val timeStampText: TextView = itemView.findViewById(R.id.textTimestamp)
        val postText: TextView = itemView.findViewById(R.id.textPost)
        val postImage: ImageView = itemView.findViewById(R.id.imagePost)
        val postVideo: VideoView = itemView.findViewById(R.id.videoPost)
        val likeIcon: ImageView = itemView.findViewById(R.id.iconLike)
        val commentIcon: ImageView = itemView.findViewById(R.id.iconComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = feedList[position]

        // Set the text content for the post
        holder.userNameText.text = item.userName
        holder.timeStampText.text = formatTimeAgo(item.timestamp)
        holder.postText.text = item.postText

        // --- Reset views and listeners for proper RecyclerView recycling ---
        holder.postImage.setOnClickListener(null)
        holder.postVideo.setOnPreparedListener(null)
        holder.postText.setOnClickListener(null)
        holder.likeIcon.setOnClickListener(null)
        holder.commentIcon.setOnClickListener(null)

        // Hide both media views by default to ensure a clean state
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE

        // --- Set up click listeners for like and comment icons ---
        holder.likeIcon.setOnClickListener {
            val clickedPost = feedList[position]
            Toast.makeText(holder.itemView.context, "Liked post by " + clickedPost.userName, Toast.LENGTH_SHORT).show()
            // TODO: Add logic here to update the like count in Firebase
        }

        holder.commentIcon.setOnClickListener {
            val clickedPost = feedList[position]
            Toast.makeText(holder.itemView.context, "Comment on post by " + clickedPost.userName, Toast.LENGTH_SHORT).show()
            // TODO: Add logic here for comments
        }

        // --- Media Display Logic ---

        // 1) Handle Image Posts
        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .fitCenter() // Correctly scales images without stretching
                .into(holder.postImage)
        }

        // 2) Handle Video Posts
        else if (!item.postVideoUrl.isNullOrEmpty()) {
            val videoUrl = item.postVideoUrl

            // Handle YouTube links by showing a thumbnail
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

                if (!thumbUrl.isNullOrEmpty()) {
                    holder.postImage.visibility = View.VISIBLE // Use the ImageView for the thumbnail
                    Glide.with(holder.itemView.context)
                        .load(thumbUrl)
                        .fitCenter()
                        .into(holder.postImage)

                    holder.postImage.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        holder.itemView.context.startActivity(intent)
                    }
                }
            }
            // Handle direct video links by playing them in the VideoView
            else {
                holder.postVideo.visibility = View.VISIBLE
                try {
                    holder.postVideo.setVideoURI(Uri.parse(videoUrl))
                    holder.postVideo.setOnPreparedListener { mp ->

                        // --- THE FIX: RESIZE LOGIC ---
                        val videoWidth = mp.videoWidth
                        val videoHeight = mp.videoHeight
                        if (videoWidth > 0 && videoHeight > 0) {
                            val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

                            // Use the width of the entire item view for a reliable calculation
                            val viewWidth = holder.itemView.width // ✅ This is the fix

                            if (viewWidth > 0) {
                                val newHeight = (viewWidth / videoAspectRatio).toInt()
                                holder.postVideo.layoutParams.height = newHeight
                                holder.postVideo.requestLayout() // Apply the new height
                            }
                        }
                        mp.isLooping = true
                        holder.postVideo.start()
                    }
                } catch (e: Exception) {
                    // Fallback if the video URL is invalid
                    holder.postVideo.visibility = View.GONE
                    holder.postText.append("\n\n▶ Invalid Video Link: $videoUrl")
                }
            }
        }
        // 3) Text-only posts are handled implicitly, as both media views remain hidden.
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