package com.example.myapplication.home // Correct package name

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
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.target.Target
import android.util.Log // Added for debugging, can be removed later
import com.bumptech.glide.load.engine.GlideException

class FeedAdapter(
    private val feedList: MutableList<FeedItem>,
    private val parentWidth: Int
) :
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
        holder.likeIcon.setOnClickListener(null) // Reset like listener
        holder.commentIcon.setOnClickListener(null) // Reset comment listener

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

        // --- NEW: Pre-size the media view if dimensions are available ---
        // This calculates the necessary height based on stored dimensions and item width
        // Pre-size the media view using the dimensions from Firebase
        if (item.mediaWidth > 0 && item.mediaHeight > 0) {
            val viewToResize: View = if (!item.postImageUrl.isNullOrEmpty()) holder.postImage else holder.postVideo
            val aspectRatio = item.mediaWidth.toFloat() / item.mediaHeight.toFloat()

            // Use the reliable parentWidth for the calculation, eliminating the need for an else block
            val newHeight = (parentWidth / aspectRatio).toInt()
            viewToResize.layoutParams.height = newHeight
            viewToResize.requestLayout()
        } else {
            // Fallback for old posts without dimensions or text-only posts: use wrap_content for image, hide video
            holder.postImage.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.postImage.requestLayout()
            holder.postVideo.layoutParams.height = 0 // Ensure video is effectively hidden if no dimensions
            holder.postVideo.requestLayout()
        }


        // --- Media Display Logic ---

        // 1) Handle Image Posts
        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            // TODO: Start shimmer effect here (e.g., holder.shimmerContainer.startShimmer())
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .fitCenter() // Correctly scales images without stretching
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        // TODO: Stop shimmer effect here
                        Log.e("FeedAdapter", "Image load failed: ${e?.message}")
                        return false // Important: return false so Glide still calls the target
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        // TODO: Stop shimmer effect here (e.g., holder.shimmerContainer.stopShimmer())
                        return false
                    }
                })
                .into(holder.postImage)
        }

        // 2) Handle Video Posts
        else if (!item.postVideoUrl.isNullOrEmpty()) { // Use else if to ensure only one media type is shown
            val videoUrl = item.postVideoUrl

            // Handle YouTube links by showing a thumbnail in the ImageView
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

                if (!thumbUrl.isNullOrEmpty()) {
                    holder.postImage.visibility = View.VISIBLE // Use the ImageView for the thumbnail
                    // TODO: Start shimmer effect here
                    Glide.with(holder.itemView.context)
                        .load(thumbUrl)
                        .fitCenter()
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                // TODO: Stop shimmer effect here
                                Log.e("FeedAdapter", "YouTube thumb load failed: ${e?.message}")
                                return false
                            }
                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                // TODO: Stop shimmer effect here
                                return false
                            }
                        })
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
                // TODO: Start shimmer effect here
                try {
                    holder.postVideo.setVideoURI(Uri.parse(videoUrl))
                    holder.postVideo.setOnPreparedListener { mp ->
                        // The height is already set by the pre-sizing logic above, so no need to resize here.
                        // We just start playback.
                        mp.isLooping = true
                        holder.postVideo.start()
                        // TODO: Stop shimmer effect here
                    }
                    // For VideoView, also handle error and completion for shimmer
                    holder.postVideo.setOnErrorListener { mp, what, extra ->
                        // TODO: Stop shimmer effect here
                        Log.e("FeedAdapter", "Video playback error: what=$what, extra=$extra")
                        holder.postVideo.visibility = View.GONE
                        holder.postText.append("\n\n▶ Error playing video: $videoUrl")
                        true // Indicate that the error was handled
                    }

                } catch (e: Exception) {
                    // Fallback if the video URL is invalid or unplayable
                    Log.e("FeedAdapter", "Direct video setup error: ${e.message}", e)
                    holder.postVideo.visibility = View.GONE
                    holder.postText.append("\n\n▶ Invalid Video Link: $videoUrl")
                    // TODO: Stop shimmer effect here
                }
            }
        }
        // Text-only posts are handled implicitly; media views remain hidden.
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