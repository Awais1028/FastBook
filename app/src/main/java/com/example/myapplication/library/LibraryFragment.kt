package com.example.myapplication.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.home.FeedActivity
import com.google.android.material.button.MaterialButton

class LibraryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Handle Window Insets (Margin for Edge-to-Edge)
        val btnBack = view.findViewById<View>(R.id.btnBack)
        // Convert 16dp (original XML margin) to pixels
        val originalMargin = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Get current layout params as MarginLayoutParams
            val params = btnBack.layoutParams as ViewGroup.MarginLayoutParams

            // Add the status bar height (insets.top) to the original 16dp margin
            params.topMargin = originalMargin + insets.top

            // Apply the updated params back to the button
            btnBack.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        // 2. Back Button Logic
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 3. "Visit Website" Button
        val btnVisitWebsite = view.findViewById<MaterialButton>(R.id.btnVisitWebsite)
        btnVisitWebsite.setOnClickListener {
            val url = "https://nu.insigniails.com/Library/Home"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. "Get Password" (Email) Box
        val btnGetPassword = view.findViewById<LinearLayout>(R.id.btnGetPassword)
        btnGetPassword.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Only email apps should handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf("librarylhr@nu.edu.pk"))
                putExtra(Intent.EXTRA_SUBJECT, "Request for Library Password")
                putExtra(Intent.EXTRA_TEXT, "Hello,\n\nI would like to request my library password.\n\nMy Roll Number is: [Enter Roll No]\n")
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Send email using..."))
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? FeedActivity)?.updateNavigationUi(showTopBar = true, showBottomBar = true)
    }
}