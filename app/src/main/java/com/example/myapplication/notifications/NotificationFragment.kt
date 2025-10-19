package com.example.myapplication.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationFragment : Fragment(R.layout.fragment_notification) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.notificationRecyclerView)
        adapter = NotificationAdapter(notificationList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId)

        notifRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                for (dataSnapshot in snapshot.children) {
                    val notification = dataSnapshot.getValue(NotificationItem::class.java)
                    notification?.let { notificationList.add(it) }
                }
                notificationList.reverse() // Show newest notifications first
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}