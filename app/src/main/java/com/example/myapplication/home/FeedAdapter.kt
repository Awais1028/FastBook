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
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import com.example.myapplication.profile.ProfileFragment
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
        val likeCountText: TextView = itemView.findViewById(R.id.like_count_text)
        var player: ExoPlayer? = null
        val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)

        // --- 👇 FIX 1: ADD THIS FUNCTION SO FRAGMENT CAN CALL IT 👇 ---
        fun startPlayer() {
            if (player != null) {
                player?.playWhenReady = true
            }
        }
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Release old player
        holder.player?.release()
        holder.player = null
        holder.postVideo.player = null
        activePlayers.remove(holder.player)

        // Comment count
        val commentText = if (item.commentCount == 1) "1 " else "${item.commentCount} "
        holder.commentCountText.text = commentText

        // Reset Views
        holder.shimmerLayout.startShimmer()
        holder.shimmerLayout.visibility = View.VISIBLE
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE

        // Reset listeners
        holder.postImage.setOnClickListener(null)
        holder.likeIcon.setOnClickListener(null)
        holder.commentIcon.setOnClickListener(null)

        // Set Text
        holder.userNameText.text = item.userName
        holder.timeStampText.text = formatTimeAgo(item.timestamp)
        holder.postText.text = item.postText

        holder.userNameText.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val clickedPost = feedList[currentPosition]
                val fragment = ProfileFragment()
                val args = Bundle()
                args.putString("uid", clickedPost.publisher)
                fragment.arguments = args
                val activity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Like System
        holder.likeCountText.text = "${item.likes.size} likes"
        val isLiked = item.likes.containsKey(currentUserId)
        if (isLiked) {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_filled)
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_heart)
        }

        holder.commentIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val clickedPost = feedList[currentPosition]
                val fragment = CommentFragment()
                val args = Bundle()
                args.putString("postId", clickedPost.postId)
                args.putString("postOwnerId", clickedPost.publisher)
                fragment.arguments = args
                val activity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        holder.likeIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION && currentUserId != null) {
                val clickedPost = feedList[currentPosition]
                val postLikesRef = FirebaseDatabase.getInstance().getReference("posts")
                    .child(clickedPost.postId).child("likes")

                if (clickedPost.likes.containsKey(currentUserId)) {
                    postLikesRef.child(currentUserId).removeValue()
                } else {
                    postLikesRef.child(currentUserId).setValue(true).addOnSuccessListener {
                        addNotification(clickedPost.publisher, currentUserId, "liked your post.", clickedPost.postId)
                    }
                }
            }
        }

        // Resize Logic
        if (item.mediaWidth > 0 && item.mediaHeight > 0) {
            val viewToResize: View = if (!item.postImageUrl.isNullOrEmpty() || (item.postVideoUrl?.contains("youtube") == true || item.postVideoUrl?.contains("youtu.be") == true)) {
                holder.postImage
            } else {
                holder.postVideo
            }
            val aspectRatio = item.mediaWidth.toFloat() / item.mediaHeight.toFloat()
            val newHeight = (parentWidth / aspectRatio).toInt()
            viewToResize.layoutParams.height = newHeight
            viewToResize.requestLayout()
        } else {
            holder.postImage.layoutParams.height = 0
            holder.postImage.requestLayout()
            holder.postVideo.layoutParams.height = 0
            holder.postVideo.requestLayout()
        }

        // Media Display
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
                // YouTube handling...
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
            } else {
                // Direct Video (ExoPlayer)
                holder.postVideo.visibility = View.VISIBLE
                try {
                    holder.player = ExoPlayer.Builder(holder.itemView.context).build().apply {
                        setMediaItem(MediaItem.fromUri(videoUrl))
                        repeatMode = ExoPlayer.REPEAT_MODE_ONE

                        // --- 👇 FIX 2: SET THIS TO FALSE SO THEY DON'T ALL START PLAYING 👇 ---
                        playWhenReady = false

                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    holder.shimmerLayout.stopShimmer()
                                    holder.shimmerLayout.hideShimmer()
                                }
                            }
                            override fun onPlayerError(error: PlaybackException) {
                                Log.e("FeedAdapter", "ExoPlayer error: ${error.message}", error)
                                holder.postVideo.visibility = View.GONE
                                holder.shimmerLayout.stopShimmer()
                                holder.shimmerLayout.hideShimmer()
                            }
                        })
                        prepare()
                    }
                    holder.postVideo.player = holder.player
                    activePlayers.add(holder.player!!)
                } catch (e: Exception) {
                    Log.e("FeedAdapter", "ExoPlayer setup error: ${e.message}", e)
                    holder.postVideo.visibility = View.GONE
                    holder.shimmerLayout.stopShimmer()
                    holder.shimmerLayout.hideShimmer()
                }
            }
        } else {
            holder.shimmerLayout.stopShimmer()
            holder.shimmerLayout.hideShimmer()
        }
    }

    private fun addNotification(postOwnerId: String, actorId: String, text: String, postId: String) {
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

    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.let {
            it.release()
            activePlayers.remove(it)
        }
        holder.player = null
        holder.postVideo.player = null
        holder.shimmerLayout.stopShimmer()
        holder.shimmerLayout.hideShimmer()
    }

    // --- Lifecycle Methods ---

    fun pauseAllPlayers() {
        for (player in activePlayers) {
            player.playWhenReady = false // Ensure we pause, not release here if we want to resume later
        }
        Log.d("FeedAdapter", "Paused ${activePlayers.size} players.")
    }

    fun resumeAllPlayers() {
        // --- 👇 FIX 3: DO NOT BLINDLY PLAY ALL. LEFT EMPTY ON PURPOSE 👇 ---
        // The FeedFragment calculates which SINGLE video to play and calls startPlayer().
        // If we put code here, it will mess up that logic.
    }

    fun releaseAllPlayers() {
        for (player in activePlayers) {
            player.release()
        }
        activePlayers.clear()
        Log.d("FeedAdapter", "Released all players.")
    }

    override fun getItemCount(): Int = feedList.size

    private fun createShimmerListener(holder: FeedViewHolder): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
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