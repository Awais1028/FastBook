package com.example.myapplication.notifications

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class NotificationFragment : Fragment(R.layout.fragment_notification) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.notificationRecyclerView)

        val sampleData = listOf(
            NotificationItem("New Post", "Ali just posted a new image."),
            NotificationItem("Like", "Jack and 20 others liked your post."),
            NotificationItem("Reminder", "You haven't updated your password since last month.")
        )

        adapter = NotificationAdapter(sampleData)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
}
