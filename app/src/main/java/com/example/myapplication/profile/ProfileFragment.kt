package com.example.myapplication.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.home.FeedAdapter
import com.example.myapplication.home.FeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var profileUserId: String? = null
    private var currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // UI Views
    private lateinit var fullName: TextView
    private lateinit var editProfileButton: Button
    private lateinit var followButton: Button
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView

    // For "My Posts" feature
    private lateinit var myPostsRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<FeedItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This logic correctly determines whose profile to show.
        profileUserId = arguments?.getString("uid") ?: currentUserId

        // Add a log to be 100% sure we are receiving the correct ID
        Log.d("ProfileDebug", "Displaying profile for user ID: $profileUserId")

        // Initialize all views
        fullName = view.findViewById(R.id.profile_full_name)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        followButton = view.findViewById(R.id.follow_unfollow_button)
        followersCount = view.findViewById(R.id.followers_count)
        followingCount = view.findViewById(R.id.following_count)
        myPostsRecyclerView = view.findViewById(R.id.my_posts_recycler_view)

        // Setup for the user's posts
        myPostsRecyclerView.layoutManager = LinearLayoutManager(context)
        myPostsRecyclerView.post {
            val parentWidth = myPostsRecyclerView.width
            if (parentWidth > 0) {
                feedAdapter = FeedAdapter(postList, parentWidth)
                myPostsRecyclerView.adapter = feedAdapter
                loadUserPosts()
            }
        }

        // Logic to show/hide the correct buttons
        if (profileUserId == currentUserId) {
            editProfileButton.visibility = View.VISIBLE
            followButton.visibility = View.GONE
        } else {
            editProfileButton.visibility = View.GONE
            followButton.visibility = View.VISIBLE
            checkFollowStatus()
        }

        // Set up click listeners
        editProfileButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, EditProfileFragment()) // Navigate to your EditProfileFragment
                .addToBackStack(null) // Allows the user to press back to return to this screen
                .commit()
        }
        followButton.setOnClickListener {
            if (followButton.text.toString().equals("Follow", ignoreCase = true)) {
                followUser()
            } else {
                unfollowUser()
            }
        }

        // Load all user data
        loadUserInfo()
        getFollowerAndFollowingCount()
    }

    private fun loadUserInfo() {
        if (profileUserId == null) return

        // ✅ BUG FIX: This now correctly uses 'profileUserId' to fetch the name.
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(profileUserId!!)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val loadedName = snapshot.child("fullName").getValue(String::class.java)
                    fullName.text = loadedName
                    Log.d("ProfileDebug", "Loaded name: $loadedName for user ID: $profileUserId")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getFollowerAndFollowingCount() {
        if (profileUserId == null) return

        // ✅ BUG FIX: This also now correctly uses 'profileUserId'.
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(profileUserId!!)

        userRef.child("followers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followersCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        userRef.child("following").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- (The rest of your functions: loadUserPosts, onPause, followUser, etc., are correct) ---
    private fun loadUserPosts() {
        if (profileUserId == null) return
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val query = postsRef.orderByChild("publisher").equalTo(profileUserId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(FeedItem::class.java)
                    post?.let { postList.add(it) }
                }
                postList.reverse()
                if (::feedAdapter.isInitialized) {
                    feedAdapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onPause() { super.onPause(); if (::feedAdapter.isInitialized) feedAdapter.pauseAllPlayers() }
    override fun onResume() { super.onResume(); if (::feedAdapter.isInitialized) feedAdapter.resumeAllPlayers() }
    override fun onDestroyView() { super.onDestroyView(); if (::feedAdapter.isInitialized) feedAdapter.releaseAllPlayers() }

    private fun followUser() {
        if (currentUserId == null || profileUserId == null) return
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!).child("following").child(profileUserId!!).setValue(true)
        FirebaseDatabase.getInstance().getReference("Users").child(profileUserId!!).child("followers").child(currentUserId!!).setValue(true)
    }

    private fun unfollowUser() {
        if (currentUserId == null || profileUserId == null) return
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!).child("following").child(profileUserId!!).removeValue()
        FirebaseDatabase.getInstance().getReference("Users").child(profileUserId!!).child("followers").child(currentUserId!!).removeValue()
    }

    private fun checkFollowStatus() {
        if (currentUserId == null || profileUserId == null) return
        val followingRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!).child("following")
        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(profileUserId!!).exists()) {
                    followButton.text = "Following"
                } else {
                    followButton.text = "Follow"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}