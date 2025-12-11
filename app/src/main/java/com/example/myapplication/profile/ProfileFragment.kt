package com.example.myapplication.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.home.FeedAdapter
import com.example.myapplication.home.FeedItem
import com.example.myapplication.home.PostDetailFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView // Import this if you use CircleImageView in XML

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var profileUserId: String? = null
    private var currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // UI Views
    // Using View (or ImageView) is fine, but CircleImageView is better if your XML uses it
    private lateinit var profileImage: ImageView
    private lateinit var fullName: TextView
    private lateinit var editProfileButton: Button
    private lateinit var followButton: Button
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var followersLayout: LinearLayout
    private lateinit var followingLayout: LinearLayout
    private lateinit var myPostsRecyclerView: RecyclerView
    private lateinit var gridAdapter: ProfileGridAdapter
    private val postList = mutableListOf<FeedItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Padding Fix (Keep this)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }

        profileUserId = arguments?.getString("uid") ?: currentUserId

        // Initialize Views
        // Ensure your XML ID is actually R.id.profile_image
        profileImage = view.findViewById(R.id.profile_image)
        fullName = view.findViewById(R.id.profile_full_name)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        followButton = view.findViewById(R.id.follow_unfollow_button)
        followersCount = view.findViewById(R.id.followers_count)
        followingCount = view.findViewById(R.id.following_count)
        myPostsRecyclerView = view.findViewById(R.id.my_posts_recycler_view)

        // Setup Posts
        myPostsRecyclerView.layoutManager = GridLayoutManager(context, 3)
        myPostsRecyclerView.post {
            val parentWidth = myPostsRecyclerView.width
            if (parentWidth > 0) {
                gridAdapter = ProfileGridAdapter(requireContext(), postList) { post ->
                    openPostDetail(post.postId)
                }
                myPostsRecyclerView.adapter = gridAdapter
                loadUserPosts()

            }
        }
        myPostsRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val spanCount = 3
            val spacing = resources.displayMetrics.density.toInt() * 2
            val size = (myPostsRecyclerView.width - spacing) / spanCount

            for (i in 0 until myPostsRecyclerView.childCount) {
                myPostsRecyclerView.getChildAt(i)?.layoutParams?.apply {
                    height = size
                    width = size
                }
            }
        }

        followersLayout = view.findViewById(R.id.followers_layout)
        followingLayout = view.findViewById(R.id.following_layout)
        followersLayout.setOnClickListener { openListFragment("Followers") }
        followingLayout.setOnClickListener { openListFragment("Following") }

        // Button Visibility Logic
        if (profileUserId == currentUserId) {
            editProfileButton.visibility = View.VISIBLE
            followButton.visibility = View.GONE
        } else {
            editProfileButton.visibility = View.GONE
            followButton.visibility = View.VISIBLE
            checkFollowStatus()
        }

        editProfileButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        followButton.setOnClickListener {
            if (followButton.text.toString().equals("Follow", ignoreCase = true)) {
                followUser()
            } else {
                unfollowUser()
            }
        }

        // Load Data
        loadUserInfo()
        getFollowerAndFollowingCount()
    }
    private fun openPostDetail(postId: String?) {
        if (postId == null) return

        val fragment = PostDetailFragment()
        val args = Bundle()
        args.putString("postId", postId)
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadUserInfo() {
        if (profileUserId == null) return

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(profileUserId!!)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 1. Load Name
                    val loadedName = snapshot.child("fullName").getValue(String::class.java)
                    fullName.text = loadedName

                    // 2. Load Image (CORRECT KEY: "profileImageUrl")
                    val loadedImage = snapshot.child("profileImageUrl").getValue(String::class.java)

                    // 3. Load with Glide
                    if (context != null && !loadedImage.isNullOrEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(loadedImage)
                            .circleCrop()
                            .placeholder(R.drawable.profile_placeholder) // Make sure you have a placeholder drawable
                            .into(profileImage)
                    } else {
                        // Optional: Set default image if URL is missing
                        // profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- Rest of your functions remain exactly the same ---
    private fun openListFragment(title: String) {
        val fragment = FollowListFragment()
        val args = Bundle()
        args.putString("id", profileUserId)
        args.putString("title", title)
        fragment.arguments = args
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun getFollowerAndFollowingCount() {
        if (profileUserId == null) return
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
                if (::gridAdapter.isInitialized) {
                    gridAdapter.notifyDataSetChanged()
                }

            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
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