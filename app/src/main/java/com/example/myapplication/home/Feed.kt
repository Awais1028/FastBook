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

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    private lateinit var postsRef: DatabaseReference
    private var postsListener: ValueEventListener? = null

    private val feedList = mutableListOf<FeedItem>()
    private lateinit var adapter: FeedAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.feedRecyclerView) // Assigns to the class property
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // We use a post block to ensure the layout has been measured
        recyclerView.post {
            val parentWidth = recyclerView.width
            if (parentWidth > 0) {
                // Initialize the adapter with the measured width
                adapter = FeedAdapter(feedList, parentWidth)
                recyclerView.adapter = adapter
                loadPosts() // Load posts only after the adapter is set
            }
        }
    }
    private fun loadPosts() {
        postsRef = FirebaseDatabase.getInstance().getReference("posts") // Initialization happens here
        val postsQuery = postsRef.orderByChild("timestamp")

        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedList.clear()
                for (child in snapshot.children) {
                    val post = child.getValue(FeedItem::class.java)
                    post?.let { feedList.add(it) }
                }
                feedList.reverse()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "posts listener cancelled: ${error.message}")
            }
        }
        postsQuery.addValueEventListener(postsListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Add this check to prevent the crash
        if (this::postsRef.isInitialized && postsListener != null) {
            postsRef.removeEventListener(postsListener!!)

        }
    }
}