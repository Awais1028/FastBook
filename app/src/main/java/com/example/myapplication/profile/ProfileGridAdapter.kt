package com.example.myapplication.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.home.FeedItem

class ProfileGridAdapter(
    private val context: Context,
    private val posts: List<FeedItem>,
    private val onPostClick: (FeedItem) -> Unit   // 👈 ADD THIS
) : RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder>() {

    inner class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.grid_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_profile_grid, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val post = posts[position]
        val imageUrl = post.postImageUrl ?: post.postVideoUrl

        Glide.with(context)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_profile)
            .into(holder.image)

        // 👇 THIS IS WHAT YOU WERE LOOKING FOR
        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    override fun getItemCount(): Int = posts.size
}