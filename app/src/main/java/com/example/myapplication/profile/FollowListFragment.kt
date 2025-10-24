package com.example.myapplication.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.UserAdapter
import com.example.myapplication.users.Users
import com.google.firebase.database.*

class FollowListFragment : Fragment() {

    private var userId: String? = null
    private var title: String? = null
    private var idList: MutableList<String>? = null

    private lateinit var recyclerView: RecyclerView
    private var userAdapter: UserAdapter? = null
    private var userList: MutableList<Users>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_follow_list, container, false)

        userId = arguments?.getString("id")
        title = arguments?.getString("title")

        val toolbar: Toolbar = view.findViewById(R.id.follow_list_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = title
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        recyclerView = view.findViewById(R.id.follow_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        userList = mutableListOf()
        userAdapter = UserAdapter(requireContext(), userList!!, false)
        recyclerView.adapter = userAdapter

        idList = mutableListOf()

        loadIdList()

        return view
    }

    private fun loadIdList() {
        val path = if (title == "Following") "following" else "followers"
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(userId!!).child(path)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                idList?.clear()
                for (dataSnapshot in snapshot.children) {
                    idList?.add(dataSnapshot.key!!)
                }
                loadUserList()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadUserList() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList?.clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(Users::class.java)
                    if (user != null && idList?.contains(user.getUID()) == true) {
                        userList?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}