package com.example.myapplication.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.collections.HashMap
import kotlin.math.abs

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    private val allPosts = mutableListOf<FeedItem>()
    private val feedList = mutableListOf<FeedItem>()

    private lateinit var postsRef: DatabaseReference
    private var postsListener: ValueEventListener? = null

    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var pendingQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.feedRecyclerView)
        layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, 0, 0, 250)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500)

        setFragmentResultListener("post_update") { _, bundle ->
            val postId = bundle.getString("updatedPostId") ?: return@setFragmentResultListener
            val newCount = bundle.getInt("newCommentCount")

            val index = feedList.indexOfFirst { it.postId == postId }
            if (index != -1) {
                feedList[index] = feedList[index].copy(commentCount = newCount)
                adapter.notifyItemChanged(index)
            }
        }

        setFragmentResultListener("feed_refresh") { _, _ ->
            loadPosts()
        }

        recyclerView.post {
            val parentWidth = recyclerView.width
            if (parentWidth > 0) {
                adapter = FeedAdapter(feedList, parentWidth)
                recyclerView.adapter = adapter
                recyclerView.post { playVisibleVideo() }

                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            playVisibleVideo()
                        }
                    }
                })

                pendingQuery?.let {
                    pendingQuery = null
                    onSearchQuery(it)
                }

                loadPosts()
            }
        }

        swipeRefreshLayout.setOnRefreshListener { loadPosts() }
    }

    private fun loadPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        postsRef = FirebaseDatabase.getInstance().getReference("posts")

        val interestsRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(currentUserId)
            .child("interests")

        interestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val scores = HashMap<String, Int>()
                for (c in snapshot.children) {
                    scores[c.key ?: ""] = c.getValue(Int::class.java) ?: 0
                }
                fetchAndSortPosts(scores)
            }

            override fun onCancelled(error: DatabaseError) {
                fetchAndSortPosts(HashMap())
            }
        })
    }

    private fun fetchAndSortPosts(userScores: HashMap<String, Int>) {
        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                allPosts.clear()
                feedList.clear()

                for (child in snapshot.children) {
                    val post = child.getValue(FeedItem::class.java)
                    if (post != null) allPosts.add(post)
                }

                val sorted = allPosts.sortedWith(
                    compareByDescending<FeedItem> {
                        userScores[it.category] ?: 0
                    }.thenByDescending { it.timestamp }
                )

                feedList.addAll(sorted)
                adapter.notifyDataSetChanged()

                recyclerView.post { playVisibleVideo() }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
                Log.e(TAG, error.message)
            }
        }

        postsRef.addListenerForSingleValueEvent(postsListener!!)
    }

    fun onSearchQuery(query: String) {
        if (!::adapter.isInitialized) {
            pendingQuery = query
            return
        }

        if (query.isBlank()) {
            feedList.clear()
            feedList.addAll(allPosts)
            adapter.notifyDataSetChanged()
            return
        }

        val q = query.lowercase()

        val filtered = allPosts
            .mapNotNull { post ->
                var score = 0
                if (post.category.lowercase().contains(q)) score += 3
                if (post.postText.lowercase().contains(q)) score += 2
                if (post.userName.lowercase().contains(q)) score += 1
                if (score > 0) Pair(post, score) else null
            }
            .sortedWith(
                compareByDescending<Pair<FeedItem, Int>> { it.second }
                    .thenByDescending { it.first.timestamp }
            )
            .map { it.first }

        feedList.clear()
        feedList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    // ✅ FIXED VIDEO PLAY LOGIC (CENTER-BASED)
    private fun playVisibleVideo() {
        if (!::adapter.isInitialized) return

        adapter.pauseAllPlayers()

        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return

        val recyclerCenterY = recyclerView.height / 2

        var bestHolder: FeedAdapter.FeedViewHolder? = null
        var smallestDistance = Int.MAX_VALUE

        for (i in first..last) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i)
            if (holder is FeedAdapter.FeedViewHolder && holder.videoUrl != null) {

                val location = IntArray(2)
                holder.itemView.getLocationOnScreen(location)
                val itemTop = location[1]
                val itemCenterY = itemTop + holder.itemView.height / 2

                val distance = abs(itemCenterY - recyclerCenterY)
                if (distance < smallestDistance) {
                    smallestDistance = distance
                    bestHolder = holder
                }
            }
        }

        bestHolder?.startPlayer(feedList[bestHolder.adapterPosition].category)
    }

    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) adapter.pauseAllPlayers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) adapter.releaseAllPlayers()
        postsListener?.let { postsRef.removeEventListener(it) }
    }
}
