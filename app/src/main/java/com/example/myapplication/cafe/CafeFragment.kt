package com.example.myapplication.cafe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.home.FeedActivity

class CafeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cafeMenuAdapter: CafeMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the new layout
        val view = inflater.inflate(R.layout.fragment_cafe, container, false)

        // 1. Setup Custom Back Button
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Handle Window Insets (FIX: Use Margin so button isn't squashed)
        // Convert 16dp (original XML margin) to pixels
        val originalMargin = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Get current layout params as MarginLayoutParams
            val params = btnBack.layoutParams as ViewGroup.MarginLayoutParams

            // Add status bar height (insets.top) to the original 16dp margin
            params.topMargin = originalMargin + insets.top

            // Apply the updated params back to the button
            btnBack.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        // 3. Create Menu Data
        val fullMenu = mutableListOf<CafeDisplayItem>()

        fullMenu.add(CafeCategory("Chinese"))
        fullMenu.add(CafeMenuItem("Macaroni", R.drawable.macroni))
        fullMenu.add(CafeMenuItem("Chowmein", R.drawable.chowmein))

        fullMenu.add(CafeCategory("Vegetable Gravy"))
        fullMenu.add(CafeMenuItem("Haleem", R.drawable.haleem))
        fullMenu.add(CafeMenuItem("Kari Chawal", R.drawable.kari_chawal))

        fullMenu.add(CafeCategory("Karahi"))
        fullMenu.add(CafeMenuItem("Boneless Karahi", R.drawable.bonelesskarahi))
        fullMenu.add(CafeMenuItem("White Karahi", R.drawable.white_karahi))

        fullMenu.add(CafeCategory("Fast Food"))
        fullMenu.add(CafeMenuItem("Zinger Burger", R.drawable.zingerburger))
        fullMenu.add(CafeMenuItem("Shawarma", R.drawable.shawarma))
        fullMenu.add(CafeMenuItem("Sandwich", R.drawable.sandwich))
        fullMenu.add(CafeMenuItem("Shami Burger", R.drawable.andashamiburger))

        // 4. Setup RecyclerView (Grid)
        recyclerView = view.findViewById(R.id.cafe_menu_recycler_view)
        cafeMenuAdapter = CafeMenuAdapter(fullMenu)

        val gridLayoutManager = GridLayoutManager(context, 2)

        // Logic: Headers take 2 columns (Full Width), Items take 1 column
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (cafeMenuAdapter.getItemViewType(position) == CafeMenuAdapter.TYPE_HEADER) 2 else 1
            }
        }

        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = cafeMenuAdapter

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}