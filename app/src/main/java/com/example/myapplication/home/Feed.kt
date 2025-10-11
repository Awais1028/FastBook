package com.example.myapplication.home

import FeedItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FeedFragment : Fragment() {

    private val TAG = "FeedFragment"

    private lateinit var postsRef: DatabaseReference
    private var postsListener: ValueEventListener? = null

    // mutable list so we can update it when data arrives
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

        recyclerView = view.findViewById(R.id.feedRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize adapter with the mutable list
        adapter = FeedAdapter(feedList)
        recyclerView.adapter = adapter

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = dateFormat.format(Date())

        // 🔹 OPTIONAL: add local sample posts (remove later)
        feedList.addAll(
            listOf(
                FeedItem(userName = "Awais", timeStamp = formattedTimestamp, postText = "Just joined FASTBook!")
            )
        )
        adapter.notifyDataSetChanged()

        // 🔹 Setup Firebase DB reference and listener
        postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear and refill the feed list from DB
                feedList.clear()
                for (child in snapshot.children) {
                    val post = child.getValue(FeedItem::class.java)
                    post?.let { feedList.add(it) }
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                feedList.sortByDescending {
                    dateFormat.parse(it.timeStamp)?.time ?: 0L
                }
                // 🔹 Sort by timeStamp so newest shows first
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "posts listener cancelled: ${error.message}")
            }
        }
        postsRef.addValueEventListener(postsListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // detach listener to prevent memory leaks
        postsListener?.let { postsRef.removeEventListener(it) }
    }
}
