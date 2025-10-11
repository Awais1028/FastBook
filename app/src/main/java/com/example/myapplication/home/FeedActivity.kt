package com.example.myapplication.home

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.post.NewPostFragment
import com.example.myapplication.notifications.NotificationFragment
import com.example.myapplication.profile.ProfileFragment
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed) // layout with a container for the fragment
        supportActionBar?.title = ""
        // Enable edge-to-edge content
        ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars()) // Hide status bar for full screen
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val topBar = findViewById<View>(R.id.topBar)
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currentFragment is ProfileFragment) {
                topBar.visibility = View.GONE
            } else {
                topBar.visibility = View.VISIBLE
            }
        }

        // Set up the Toolbar (with 3-dotted menu)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Replace fragment if it's the first time loading the activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, FeedFragment()) // Assuming container is the ID of your FrameLayout
                .commit()
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, FeedFragment())
                        .commit()
                    topBar.visibility = View.VISIBLE
                    true
                }

                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.nav_notifications ->{
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, NotificationFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.nav_add ->{
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, NewPostFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                // Add other items similarly if needed
                else -> false
            }
        }
    }

    // Inflate the 3-dotted options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Inflate the 3-dotted menu
        return true
    }

    // Handle options menu item selections
    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                // Handle Home option here
                true
            }
            R.id.nav_profile -> {
                val profileFragment = ProfileFragment()

                Log.d("FeedActivity", "Attempting to replace with ProfileFragment")

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.container, profileFragment)  // Ensure container ID is correct
                transaction.addToBackStack(null) // Optional
                transaction.commit()

                Log.d("FeedActivity", "ProfileFragment replaced successfully")

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }*/
}