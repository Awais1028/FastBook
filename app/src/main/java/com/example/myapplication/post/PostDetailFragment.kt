package com.example.myapplication.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.database.*
import com.example.myapplication.home.FeedAdapter // ✅ IMPORT ADDED
import com.example.myapplication.home.FeedItem   // ✅ IMPORT ADDED

class PostDetailFragment : Fragment() {

    private var postId: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val postList = mutableListOf<FeedItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_post_detail, container, false)

        // Get the postId that was passed from the notification
        postId = arguments?.getString("postId")

        // Set up the toolbar with a back button
        val toolbar: Toolbar = view.findViewById(R.id.post_detail_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Post"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.post_detail_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Measure width and initialize adapter, just like in FeedFragment
        recyclerView.post {
            val parentWidth = recyclerView.width
            if (parentWidth > 0) {
                adapter = FeedAdapter(postList, parentWidth)
                recyclerView.adapter = adapter
                loadPost() // Load the single post
            }
        }

        return view
    }

    private fun loadPost() {
        if (postId == null) {
            Log.e("PostDetailFragment", "Error: postId is null.")
            return
        }

        val postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId!!)

        // Use addListenerForSingleValueEvent because we only need to load it once
        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                val post = snapshot.getValue(FeedItem::class.java)
                if (post != null) {
                    postList.add(post)
                }
                if (::adapter.isInitialized) {
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("PostDetailFragment", "Failed to load post: ${error.message}")
            }
        })
    }

    // It's crucial to manage the video player's lifecycle here too
    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) adapter.pauseAllPlayers()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) adapter.resumeAllPlayers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) adapter.releaseAllPlayers()
    }
}