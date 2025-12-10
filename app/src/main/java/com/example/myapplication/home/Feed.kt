package com.example.myapplication.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.database.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // Add this import

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    private lateinit var postsRef: DatabaseReference
    private var postsListener: ValueEventListener? = null

    private val feedList = mutableListOf<FeedItem>()
    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

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

        recyclerView.post {
            if (view.context == null) return@post

            val parentWidth = recyclerView.width
            if (parentWidth > 0) {
                adapter = FeedAdapter(feedList, parentWidth)
                recyclerView.adapter = adapter

                // 1. Add Scroll Listener to detect when scrolling stops
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            // Only play video when scrolling stops
                            playVisibleVideo()
                        }
                    }
                })

                loadPosts()
            } else {
                Log.e(TAG, "RecyclerView width is 0. Cannot initialize adapter.")
            }
        }
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        // Set the refresh color (optional, matches your app theme)
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500) // Or whatever primary color you have

        swipeRefreshLayout.setOnRefreshListener {
            // Call your existing function to reload data
            loadPosts()
        }
    }

    private fun loadPosts() {
        postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val postsQuery = postsRef.orderByChild("timestamp")

        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedList.clear()
                for (child in snapshot.children) {
                    val post = child.getValue(FeedItem::class.java)
                    post?.let { feedList.add(it) }
                }
                feedList.reverse()
                if (::adapter.isInitialized) {
                    adapter.notifyDataSetChanged()
                    // 2. Play the first video automatically when data loads
                    recyclerView.post { playVisibleVideo() }
                }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    view?.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false

                    // Optional: Show a little toast so they know it worked
                    // Toast.makeText(context, "Feed Updated", Toast.LENGTH_SHORT).show()
                }, 1000)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "posts listener cancelled: ${error.message}")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    view?.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false
                }, 1000)
            }
        }
        postsQuery.addListenerForSingleValueEvent(postsListener!!)
    }

    // --- NEW LOGIC: Play ONLY the single most visible video ---
    private fun playVisibleVideo() {
        if (!::adapter.isInitialized) return

        // 1. First, pause ALL players to stop the "all playing" issue
        adapter.pauseAllPlayers()

        // 2. Find visible range
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible < 0) return

        // 3. Find the "best" item (the one taking up the most screen space)
        var bestPosition = -1
        var maxPercentage = 0

        for (i in firstVisible..lastVisible) {
            val view = layoutManager.findViewByPosition(i) ?: continue

            // Calculate how much of the item is actually visible
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val viewTop = location[1]
            val viewBottom = viewTop + view.height
            val screenHeight = resources.displayMetrics.heightPixels

            // Basic visibility check (simplified for stability)
            val visibleTop = maxOf(viewTop, 0)
            val visibleBottom = minOf(viewBottom, screenHeight)
            val visibleHeight = maxOf(0, visibleBottom - visibleTop)

            val percentage = if (view.height > 0) (visibleHeight * 100) / view.height else 0

            // If item is more than 50% visible, it's our candidate
            if (percentage >= 50) {
                bestPosition = i
                break // Stop at the first mostly visible item (top-down)
            }
        }

        // 4. Play only that winner
        if (bestPosition != -1) {
            val holder = recyclerView.findViewHolderForAdapterPosition(bestPosition)
            // You might need to cast this if your holder class is named differently
            if (holder is FeedAdapter.FeedViewHolder) {
                // Ensure your FeedViewHolder has a 'play()' or similar method exposed
                // If not, you might need to add one or access the player directly if public
                holder.startPlayer()
            }
        }
    }

    // --- Lifecycle Fixes ---

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (::adapter.isInitialized) {
            if (hidden) {
                adapter.pauseAllPlayers()
            } else {
                // CHANGED: Instead of resumeAllPlayers(), verify visibility first
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
        if (::adapter.isInitialized && !isHidden) {
            // CHANGED: Play only visible
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