package com.example.myapplication.home

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FeedAdapter(
    private val feedList: MutableList<FeedItem>,
    private val parentWidth: Int
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

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
        val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
        val bigHeart: ImageView = itemView.findViewById(R.id.big_heart)
        val touchOverlay: View = itemView.findViewById(R.id.touch_overlay)

        var player: ExoPlayer? = null
        var videoUrl: String? = null

        // 👇 Watch Timer Logic
        private var watchTimerHandler = Handler(Looper.getMainLooper())
        private var watchRunnable: Runnable? = null
        private var hasCountedView = false

        fun startPlayer(category: String) {
            releaseAllPlayers()

            // Reset tracking for this playback session
            hasCountedView = false
            watchRunnable?.let { watchTimerHandler.removeCallbacks(it) }

            if (player == null && !videoUrl.isNullOrEmpty()) {
                try {
                    player = ExoPlayer.Builder(itemView.context).build().apply {
                        setMediaItem(MediaItem.fromUri(videoUrl!!))
                        repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        playWhenReady = true

                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    shimmerLayout.stopShimmer()
                                    shimmerLayout.hideShimmer()
                                    postImage.visibility = View.GONE

                                    // Start the 10-second timer
                                    startWatchTimer(category)
                                }
                            }
                            override fun onPlayerError(error: PlaybackException) {
                                Log.e("FeedAdapter", "ExoPlayer error: ${error.message}", error)
                            }
                        })
                        prepare()
                    }
                    postVideo.player = player
                    postVideo.visibility = View.VISIBLE
                    player?.let { activePlayers.add(it) }

                } catch (e: Exception) {
                    Log.e("FeedAdapter", "Error initializing player: ${e.message}")
                }
            } else {
                player?.playWhenReady = true
                startWatchTimer(category)
            }
        }

        private fun startWatchTimer(category: String) {
            if (hasCountedView) return

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            watchRunnable = Runnable {
                if (!hasCountedView) {
                    // 10 seconds passed! Add +2 Points
                    Log.d("InterestSystem", "User watched > 10s. Adding +2 points to $category")
                    updateUserInterest(currentUserId, category, 2)
                    hasCountedView = true
                }
            }
            // Schedule for 10 seconds
            watchTimerHandler.postDelayed(watchRunnable!!, 10000)
        }

        fun stopWatchTimer() {
            watchRunnable?.let { watchTimerHandler.removeCallbacks(it) }
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

        holder.player?.release()
        holder.player = null
        holder.postVideo.player = null
        holder.stopWatchTimer() // Stop timer when scrolling away

        holder.videoUrl = item.postVideoUrl
        holder.commentCountText.text = if (item.commentCount == 1) "1 " else "${item.commentCount} "

        holder.shimmerLayout.startShimmer()
        holder.shimmerLayout.visibility = View.VISIBLE
        holder.postImage.visibility = View.GONE
        holder.postVideo.visibility = View.GONE

        holder.postImage.setOnClickListener(null)
        holder.touchOverlay.setOnTouchListener(null)
        holder.likeIcon.setOnClickListener(null)
        holder.commentIcon.setOnClickListener(null)

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

        // 👇 FIXED: Passing commentCount to CommentFragment
        holder.commentIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val clickedPost = feedList[currentPosition]
                if (clickedPost.postId.isNullOrEmpty()) return@setOnClickListener

                val commentSheet = CommentFragment()
                val bundle = Bundle().apply {
                    putString("postId", clickedPost.postId)
                    putString("postOwnerId", clickedPost.publisher)   // ✅ ADD THIS
                    putString("category", clickedPost.category)       // ✅ ADD THIS
                    putInt("commentCount", clickedPost.commentCount)
                }
                commentSheet.arguments = bundle

                val activity = holder.itemView.context as AppCompatActivity
                commentSheet.show(activity.supportFragmentManager, "CommentSheet")
            }
        }


        holder.likeCountText.text = "${item.likes.size} likes"
        val isLiked = item.likes.containsKey(currentUserId)
        holder.likeIcon.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart)

        holder.likeIcon.setOnClickListener {
            if (currentUserId != null) {
                toggleLike(holder, item, currentUserId)
            }
        }

        val gestureDetector = android.view.GestureDetector(holder.itemView.context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: android.view.MotionEvent): Boolean = true
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                if (currentUserId != null) {
                    if (!item.likes.containsKey(currentUserId)) {
                        toggleLike(holder, item, currentUserId)
                    }
                    animateBigHeart(holder.bigHeart)
                }
                return true
            }
            override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                if (holder.postImage.visibility == View.VISIBLE) holder.postImage.performClick()
                else if (holder.postVideo.visibility == View.VISIBLE) holder.postVideo.performClick()
                return true
            }
        })

        holder.touchOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
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
            holder.postVideo.layoutParams.height = 0
            holder.postImage.requestLayout()
            holder.postVideo.requestLayout()
        }

        if (!item.postImageUrl.isNullOrEmpty()) {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.postImageUrl)
                .fitCenter()
                .listener(createShimmerListener(holder))
                .into(holder.postImage)

        } else if (!item.postVideoUrl.isNullOrEmpty()) {
            val videoUrl = item.postVideoUrl

            if (videoUrl?.contains("youtube.com") == true || videoUrl?.contains("youtu.be") == true) {
                val videoId = extractYoutubeId(videoUrl)
                val thumbUrl = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

                holder.postImage.visibility = View.VISIBLE
                if (!thumbUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(thumbUrl)
                        .fitCenter()
                        .listener(createShimmerListener(holder))
                        .into(holder.postImage)
                }
                holder.postImage.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                    holder.itemView.context.startActivity(intent)
                }
            } else {
                holder.postImage.visibility = View.VISIBLE
                holder.postVideo.visibility = View.GONE
                Glide.with(holder.itemView.context)
                    .load(videoUrl)
                    .fitCenter()
                    .listener(createShimmerListener(holder))
                    .into(holder.postImage)
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

        val notification = NotificationItem(notificationId, actorId, text, postId, System.currentTimeMillis())

        notifRef.child(notificationId).setValue(notification)
            .addOnFailureListener { e ->
                Log.e("NotifDebug", "Failed to write notification: ${e.message}", e)
            }
    }


    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.release()
        holder.player = null
        holder.stopWatchTimer()
        activePlayers.remove(holder.player)
    }

    fun releaseAllPlayers() {
        for (player in activePlayers) {
            player.release()
        }
        activePlayers.clear()
    }

    fun pauseAllPlayers() {
        for (player in activePlayers) {
            player.playWhenReady = false
        }
    }

    fun resumeAllPlayers() {}

    private fun animateHeart(view: View) {
        view.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                    .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            }.start()
    }

    private fun animateBigHeart(view: ImageView) {
        view.alpha = 0.7f
        view.visibility = View.VISIBLE
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(400)
            .withEndAction {
                view.visibility = View.GONE
                view.scaleX = 1f; view.scaleY = 1f; view.alpha = 1f
            }.start()
    }

    private fun toggleLike(holder: FeedViewHolder, item: FeedItem, currentUserId: String) {
        val postLikesRef = FirebaseDatabase.getInstance().getReference("posts").child(item.postId).child("likes")
        animateHeart(holder.likeIcon)

        if (item.likes.containsKey(currentUserId)) {
            postLikesRef.child(currentUserId).removeValue()
            item.likes.remove(currentUserId)
            holder.likeIcon.setImageResource(R.drawable.ic_heart)
            holder.likeCountText.text = "${item.likes.size} likes"
        } else {
            postLikesRef.child(currentUserId).setValue(true).addOnSuccessListener {
                addNotification(item.publisher, currentUserId, "liked your post.", item.postId)
            }
            item.likes[currentUserId] = true
            holder.likeIcon.setImageResource(R.drawable.ic_heart_filled)
            holder.likeCountText.text = "${item.likes.size} likes"

            // Like = +1 Point
            updateUserInterest(currentUserId, item.category, 1)
        }
    }

    private fun updateUserInterest(userId: String, category: String, points: Int) {
        if (category == "General" || category.isEmpty()) return

        val interestRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(userId)
            .child("interests")
            .child(category)

        interestRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentScore = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentScore + points
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
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