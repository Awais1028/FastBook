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
        private const val TYPE_CATEGORY = 0
        private const val TYPE_MENU_ITEM = 1
    }

    // ViewHolder for Category Headers
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.category_title)
    }

    // ViewHolder for Menu Items
    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.menu_item_image)
        val itemName: TextView = itemView.findViewById(R.id.menu_item_name)
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is CafeCategory -> TYPE_CATEGORY
            is CafeMenuItem -> TYPE_MENU_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CATEGORY) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cafe_category_header, parent, false)
            CategoryViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cafe_menu_item, parent, false)
            MenuItemViewHolder(view)
        }
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]
        if (holder is CategoryViewHolder && item is CafeCategory) {
            holder.categoryTitle.text = item.title
        } else if (holder is MenuItemViewHolder && item is CafeMenuItem) {
            holder.itemName.text = item.name
            holder.itemImage.setImageResource(item.imageResId)
        }
    }
}