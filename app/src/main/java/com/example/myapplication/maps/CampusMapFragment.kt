package com.example.myapplication.maps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.home.FeedActivity

class CampusMapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_campus_map, container, false)

        // 1. Setup Custom Back Button
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Handle Window Insets (Margin for Edge-to-Edge)
        val originalMargin = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            val params = btnBack.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = originalMargin + insets.top
            btnBack.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        return view
    }
}