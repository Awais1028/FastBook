package com.example.myapplication.home

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player // Correct import for Player.Listener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.myapplication.R
import com.example.myapplication.comments.CommentFragment
import com.example.myapplication.notifications.NotificationItem
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth // Needed for like logic
import com.google.firebase.database.FirebaseDatabase // Needed for like logic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FeedAdapter(
    private val feedList: MutableList<FeedItem>,
    private val parentWidth: Int
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    // --- A list to keep track of all active ExoPlayers ---
    private val activePlayers = mutableListOf<ExoPlayer>()

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shimmerLayout: ShimmerFrameLayout = itemView.findViewById(R.id.shimmer_layout)
        val userNameText: TextView = itemView.findViewById(R.id.textUserName)
        val timeStampText: TextView = itemView.findViewById(R.id.textTimestamp)
        val postText: TextView = itemView.findViewById(R.id.textPost)
        val postImage: ImageView = itemView.findViewById(R.id.imagePost)
        val postVideo: PlayerView = itemView.findViewById(R.id.videoPost)
        val likeIcon: ImageView = itemView.findViewById(R.id.iconLike)
        val commentIcon: ImageView = itemView.findViewById(R.id.iconComment)
        val likeCountText: TextView = itemView.findViewById(R.id.like_count_text) // Added for like feature
        var player: ExoPlayer? = null // Each ViewHolder will have its own player instance
        val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
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
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = feedList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // For like functionality

        // --- IMPORTANT FIX: Release any old player and clear references ---
        // This ensures a clean state if the ViewHolder is being reused for different content.
        holder.player?.release()
        holder.player = null
        holder.postVideo.player = null // Crucial: Detach player from PlayerView
        activePlayers.remove(holder.player) // Remove from our tracking list if it was there

        //Comment count
        val commentText = if (item.commentCount == 1) "1 " else "${item.commentCount} "
        holder.commentCountText.text = commentText

        // --- Start Shimmer & Reset Views ---
        holder.shimmerLayout.startShimmer()
        holder.shimmerLayout.visibility = View.VISIBLE
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE

        // Reset click listeners (critical for RecyclerView recycling)
        holder.postImage.setOnClickListener(null)
        // No setOnPreparedListener/setOnErrorListener for PlayerView, listeners are on ExoPlayer object
        holder.likeIcon.setOnClickListener(null)
        holder.commentIcon.setOnClickListener(null)

        // --- Set Text Content ---
        holder.userNameText.text = item.userName
        holder.timeStampText.text = formatTimeAgo(item.timestamp)
        holder.postText.text = item.postText

        // --- NEW: LIKE SYSTEM LOGIC ---
        // 1. Set Like Count and Icon State
        holder.likeCountText.text = "${item.likes.size} likes"
        val isLiked = item.likes.containsKey(currentUserId)
        if (isLiked) {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_filled) // You need this drawable
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_heart)
        }

        holder.commentIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val clickedPost = feedList[currentPosition]

                // Create the CommentFragment
                val fragment = CommentFragment()

                // Create a bundle to pass the postId
                val args = Bundle()
                args.putString("postId", clickedPost.postId)
                args.putString("postOwnerId", clickedPost.publisher)
                fragment.arguments = args

                // Get the FragmentManager from the context and perform the transaction
                val activity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment) // Use your main fragment container ID from FeedActivity
                    .addToBackStack(null) // Allows the user to press "back" to return to the feed
                    .commit()
            }
        }
        // 2. Handle Like/Unlike Click
        holder.likeIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION && currentUserId != null) {
                val clickedPost = feedList[currentPosition]
                val postLikesRef = FirebaseDatabase.getInstance().getReference("posts")
                    .child(clickedPost.postId).child("likes")

                if (clickedPost.likes.containsKey(currentUserId)) {
                    // User has already liked it, so UNLIKE (no notification needed)
                    postLikesRef.child(currentUserId).removeValue()
                } else {
                    // User has not liked it, so LIKE
                    postLikesRef.child(currentUserId).setValue(true).addOnSuccessListener {
                        // ✅ NOTIFICATION IS SENT HERE on successful like
                        addNotification(clickedPost.publisher, currentUserId, "liked your post.", clickedPost.postId)
                    }
                }
            }
        }

        // --- Pre-size Media View ---
        if (item.mediaWidth > 0 && item.mediaHeight > 0) {
            val viewToResize: View = if (!item.postImageUrl.isNullOrEmpty() || (item.postVideoUrl?.contains("youtube") == true || item.postVideoUrl?.contains("youtu.be") == true)) {
                holder.postImage // Use ImageView for images and YouTube thumbnails
            } else {
                holder.postVideo // Use PlayerView for direct videos
            }
            val aspectRatio = item.mediaWidth.toFloat() / item.mediaHeight.toFloat()
            val newHeight = (parentWidth / aspectRatio).toInt()
            viewToResize.layoutParams.height = newHeight
            viewToResize.requestLayout()
        } else {
            // Fallback for old posts, text-only posts, or unretrievable dimensions: hide media views
            holder.postImage.layoutParams.height = 0
            holder.postImage.requestLayout()
            holder.postVideo.layoutParams.height = 0
            holder.postVideo.requestLayout()
        }

        // --- Media Display & Shimmer Control ---
        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .fitCenter()
                .listener(createShimmerListener(holder))
                .into(holder.postImage)
        } else if (!item.postVideoUrl.isNullOrEmpty()) {
            val videoUrl = item.postVideoUrl
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null
                if (!thumbUrl.isNullOrEmpty()) {
                    holder.postImage.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(thumbUrl)
                        .fitCenter()
                        .listener(createShimmerListener(holder))
                        .into(holder.postImage)
                    holder.postImage.setOnClickListener {
                        val currentPosition = holder.adapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            val clickedItem = feedList[currentPosition]
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clickedItem.postVideoUrl))
                            holder.itemView.context.startActivity(intent)
                        }
                    }
                } else {
                    holder.shimmerLayout.stopShimmer()
                    holder.shimmerLayout.hideShimmer()
                }
            } else { // Direct video (non-YouTube)
                holder.postVideo.visibility = View.VISIBLE
                try {
                    holder.player = ExoPlayer.Builder(holder.itemView.context).build().apply {
                        setMediaItem(MediaItem.fromUri(videoUrl))
                        repeatMode = ExoPlayer.REPEAT_MODE_ONE // Loop the video
                        playWhenReady = true // Autoplay
                        // --- Attach the Player.Listener for state changes ---
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    holder.shimmerLayout.stopShimmer()
                                    holder.shimmerLayout.hideShimmer()
                                }
                                // No specific action for STATE_ENDED as repeatMode is ONE
                            }
                            override fun onPlayerError(error: PlaybackException) {
                                Log.e("FeedAdapter", "ExoPlayer error: ${error.message}", error)
                                holder.postVideo.visibility = View.GONE // Hide video on error
                                holder.shimmerLayout.stopShimmer()
                                holder.shimmerLayout.hideShimmer()
                            }
                        })
                        prepare()
                    }
                    holder.postVideo.player = holder.player // Attach player to PlayerView
                    activePlayers.add(holder.player!!) // Add to our tracking list
                } catch (e: Exception) {
                    Log.e("FeedAdapter", "ExoPlayer setup error: ${e.message}", e)
                    holder.postVideo.visibility = View.GONE
                    holder.shimmerLayout.stopShimmer()
                    holder.shimmerLayout.hideShimmer()
                }
            }
        } else {
            // For text-only posts, stop the shimmer immediately
            holder.shimmerLayout.stopShimmer()
            holder.shimmerLayout.hideShimmer()
        }

        // --- Set Click Listeners (Using holder.adapterPosition) ---
        // (Like and Comment listeners are now above with the new logic)

        // YouTube thumbnail click listener is inside its media display block.
    }
    private fun addNotification(postOwnerId: String, actorId: String, text: String, postId: String) {
        // Don't send a notification if users interact with their own post
        if (postOwnerId == actorId) return

        val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(postOwnerId)
        val notificationId = notifRef.push().key ?: return

        val notification = NotificationItem(
            notificationId = notificationId,
            actorId = actorId,
            text = text,
            postId = postId,
            timestamp = System.currentTimeMillis()
        )

        notifRef.child(notificationId).setValue(notification)
    }
    // --- IMPORTANT FIX: Release player when ViewHolder is recycled ---
    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.let {
            it.release() // Release the player to free resources
            activePlayers.remove(it) // Remove from our tracking list
        }
        holder.player = null // Clear the player instance from the ViewHolder
        holder.postVideo.player = null // Crucial: Detach player from PlayerView
        holder.shimmerLayout.stopShimmer() // Ensure shimmer stops
        holder.shimmerLayout.hideShimmer() // Ensure shimmer hides
    }

    // --- NEW LIFECYCLE MANAGEMENT METHODS FOR FeedFragment ---
    fun pauseAllPlayers() {
        for (player in activePlayers) {
            player.pause()
        }
        Log.d("FeedAdapter", "Paused ${activePlayers.size} players.")
    }

    fun resumeAllPlayers() {
        for (player in activePlayers) {
            player.play()
        }
        Log.d("FeedAdapter", "Resumed ${activePlayers.size} players.")
    }

    fun releaseAllPlayers() {
        for (player in activePlayers) {
            player.release()
        }
        activePlayers.clear()
        Log.d("FeedAdapter", "Released all players. Active players: ${activePlayers.size}")
    }

    override fun getItemCount(): Int = feedList.size

    private fun createShimmerListener(holder: FeedViewHolder): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                Log.e("Glide", "Image load failed", e)
                holder.shimmerLayout.stopShimmer()
                holder.shimmerLayout.hideShimmer()
                return false
            }
            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                holder.shimmerLayout.stopShimmer()
                holder.shimmerLayout.hideShimmer()
                return false
            }
        }
    }

    private fun extractYoutubeId(url: String): String? {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> null
        }
    }
}