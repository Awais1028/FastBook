package com.example.myapplication.cafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class CafeMenuAdapter(private val displayList: List<CafeDisplayItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    // --- FIX: Add 'else' branch to satisfy the compiler ---
    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is CafeCategory -> TYPE_HEADER
            is CafeMenuItem -> TYPE_ITEM
            else -> throw IllegalArgumentException("Invalid Item Type") // <--- FIX HERE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            // Inflate Header XML
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.cafe_category_header, parent, false)
            CategoryViewHolder(view)
        } else {
            // Inflate Grid Card XML
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.cafe_menu_item, parent, false)
            MenuItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]

        // Bind Header
        if (holder is CategoryViewHolder && item is CafeCategory) {
            holder.categoryTitle.text = item.title
        }
        // Bind Food Card
        else if (holder is MenuItemViewHolder && item is CafeMenuItem) {
            holder.itemName.text = item.name
            holder.itemImage.setImageResource(item.imageResId)
        }
    }

    override fun getItemCount(): Int = displayList.size

    // --- ViewHolders ---

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.category_title)
    }

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.menu_item_image)
        val itemName: TextView = itemView.findViewById(R.id.menu_item_name)
    }
}