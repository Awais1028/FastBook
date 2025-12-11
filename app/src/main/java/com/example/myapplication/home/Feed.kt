package com.example.myapplication.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener // Import this
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.HashMap

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    private lateinit var postsRef: DatabaseReference
    private var postsListener: ValueEventListener? = null

    private val feedList = mutableListOf<FeedItem>()
    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.feedRecyclerView)
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        // 👇 UI FIX: Prevent "Eaten Bottom" by Navigation Bar
        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, 0, 0, 250)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500)

        // 👇 NEW CODE: Listen for updates from CommentsFragment
        setFragmentResultListener("post_update") { _, bundle ->
            val updatedPostId = bundle.getString("updatedPostId")
            val newCount = bundle.getInt("newCommentCount")

            if (updatedPostId != null) {
                // Find the post in our current list
                val index = feedList.indexOfFirst { it.postId == updatedPostId }
                if (index != -1) {
                    // Update the local list
                    // NOTE: using .copy() assuming FeedItem is a data class with val fields
                    feedList[index] = feedList[index].copy(commentCount = newCount)

                    // Notify adapter to redraw just this row (no screen flicker)
                    if (::adapter.isInitialized) {
                        adapter.notifyItemChanged(index)
                    }
                }
            }
        }

        recyclerView.post {
            if (view.context == null) return@post

            val parentWidth = recyclerView.width
            if (parentWidth > 0) {
                adapter = FeedAdapter(feedList, parentWidth)
                recyclerView.adapter = adapter

                // Scroll Listener for Auto-Play
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            playVisibleVideo()
                        }
                    }
                })

                loadPosts()
            } else {
                Log.e(TAG, "RecyclerView width is 0. Cannot initialize adapter.")
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadPosts()
        }
    }

    private fun loadPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) return

        postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val userInterestsRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserId)
            .child("interests")

        // 1. Fetch Interest Scores
        userInterestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(interestSnapshot: DataSnapshot) {
                val userScores = HashMap<String, Int>()

                for (child in interestSnapshot.children) {
                    val category = child.key ?: continue
                    val score = child.getValue(Int::class.java) ?: 0
                    userScores[category] = score
                }

                // 2. Fetch & Sort Posts
                fetchAndSortPosts(userScores)
            }

            override fun onCancelled(error: DatabaseError) {
                fetchAndSortPosts(HashMap())
            }
        })
    }

    private fun fetchAndSortPosts(userScores: HashMap<String, Int>) {
        val query = postsRef.orderByChild("timestamp")

        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedList.clear()
                for (child in snapshot.children) {
                    val post = child.getValue(FeedItem::class.java)
                    post?.let { feedList.add(it) }
                }

                // 3. Sorting Logic
                feedList.sortWith(Comparator { p1, p2 ->
                    val score1 = userScores[p1.category] ?: 0
                    val score2 = userScores[p2.category] ?: 0

                    if (score1 != score2) {
                        score2 - score1 // Priority: Interest Score
                    } else {
                        p2.timestamp.compareTo(p1.timestamp) // Priority: Time
                    }
                })

                if (::adapter.isInitialized) {
                    adapter.notifyDataSetChanged()
                    recyclerView.post { playVisibleVideo() }
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    swipeRefreshLayout.isRefreshing = false
                }, 1000)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "posts listener cancelled: ${error.message}")
                swipeRefreshLayout.isRefreshing = false
            }
        }
        query.addListenerForSingleValueEvent(postsListener!!)
    }

    // --- UPDATED: Pass Category to startPlayer ---
    private fun playVisibleVideo() {
        if (!::adapter.isInitialized) return

        adapter.pauseAllPlayers()

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible < 0) return

        var bestPosition = -1
        var maxPercentage = 0

        for (i in firstVisible..lastVisible) {
            val view = layoutManager.findViewByPosition(i) ?: continue

            // Calculate visibility percentage
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val viewTop = location[1]
            val viewBottom = viewTop + view.height
            val screenHeight = resources.displayMetrics.heightPixels

            val visibleTop = maxOf(viewTop, 0)
            val visibleBottom = minOf(viewBottom, screenHeight)
            val visibleHeight = maxOf(0, visibleBottom - visibleTop)

            val percentage = if (view.height > 0) (visibleHeight * 100) / view.height else 0

            if (percentage >= 50) {
                bestPosition = i
                break
            }
        }

        if (bestPosition != -1 && bestPosition < feedList.size) {
            val holder = recyclerView.findViewHolderForAdapterPosition(bestPosition)
            if (holder is FeedAdapter.FeedViewHolder) {
                // 👇 FIX IS HERE: Get category and pass it
                val category = feedList[bestPosition].category
                holder.startPlayer(category)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (::adapter.isInitialized) {
            if (hidden) {
                adapter.pauseAllPlayers()
            } else {
                playVisibleVideo()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) adapter.pauseAllPlayers()
    }

    override fun onResume() {
        super.onResume()

        // 1. Set UI Bars (Top: ON, Bottom: ON)
        // This replaces the manual bottomNav.visibility code

        // 2. Fix the "Half Screen" bug
        view?.requestLayout()

        // 3. Resume Video Playback
        if (::adapter.isInitialized && !isHidden) {
            playVisibleVideo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) adapter.releaseAllPlayers()
        if (::postsRef.isInitialized && postsListener != null) {
            postsRef.removeEventListener(postsListener!!)
        }
    }
}