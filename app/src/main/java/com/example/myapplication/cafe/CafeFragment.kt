package com.example.myapplication.cafe

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

class CafeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cafeMenuAdapter: CafeMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cafe, container, false) // Ensure your layout is named fragment_cafe.xml

        // Set up the toolbar
        val toolbar: Toolbar = view.findViewById(R.id.cafe_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Cafe Menu"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        // --- Create the full, categorized menu list ---
        val fullMenu = mutableListOf<CafeDisplayItem>()

        fullMenu.add(CafeCategory("Chinese"))
        fullMenu.add(CafeMenuItem("Macaroni", R.drawable.macroni))
        fullMenu.add(CafeMenuItem("Chowmein", R.drawable.chowmein))

        fullMenu.add(CafeCategory("Vegetable Gravy"))
        fullMenu.add(CafeMenuItem("Haleem", R.drawable.haleem))
        fullMenu.add(CafeMenuItem("Kari Chawal", R.drawable.karichawal))

        fullMenu.add(CafeCategory("Karahi"))
        fullMenu.add(CafeMenuItem("Boneless Karahi", R.drawable.bonelesskarahi))
        fullMenu.add(CafeMenuItem("White Karahi", R.drawable.white_karahi))

        fullMenu.add(CafeCategory("Fast Food"))
        fullMenu.add(CafeMenuItem("Zinger Burger", R.drawable.zingerburger))
        fullMenu.add(CafeMenuItem("Shawarma", R.drawable.shawarma))
        fullMenu.add(CafeMenuItem("Sandwich", R.drawable.sandwich))
        fullMenu.add(CafeMenuItem("Shami Burger", R.drawable.andashamiburger))

        // --- Set up the RecyclerView ---
        recyclerView = view.findViewById(R.id.cafe_menu_recycler_view) // Ensure this ID is in fragment_cafe.xml
        recyclerView.layoutManager = LinearLayoutManager(context)
        cafeMenuAdapter = CafeMenuAdapter(fullMenu)
        recyclerView.adapter = cafeMenuAdapter

        return view
    }
}