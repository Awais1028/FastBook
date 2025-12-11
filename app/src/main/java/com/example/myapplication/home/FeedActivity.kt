package com.example.myapplication.home

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.cafe.CafeFragment
import com.example.myapplication.chats.ChatsFragment
import com.example.myapplication.library.LibraryFragment
import com.example.myapplication.maps.CampusMapFragment
import com.example.myapplication.notifications.NotificationFragment
import com.example.myapplication.offices.FacultyMapFragment
import com.example.myapplication.post.NewPostFragment
import com.example.myapplication.profile.EditProfileFragment
import com.example.myapplication.profile.ProfileFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedActivity : AppCompatActivity() {

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var topBar: View
    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    private var feedFragment: Fragment? = null
    private var profileFragment: Fragment? = null
    private var notificationFragment: Fragment? = null
    private var newPostFragment: Fragment? = null
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_feed)

        appBarLayout = findViewById(R.id.appBarLayout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        topBar = findViewById(R.id.topBar)
        container = findViewById(R.id.container)
        bottomNav = findViewById(R.id.bottom_navigation)
        val messageIcon = findViewById<ImageView>(R.id.messageIcon)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // 1. FIX TOP: Handle Toolbar Padding
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }

        // 2. FIX BOTTOM: Handle Container Padding (Dynamic Math)
        // We wait for the bottomNav to be measured, then apply padding
        bottomNav.post {
            ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Calculate total bottom space needed:
                // System Bar (Black bar) + Bottom Menu Height
                val bottomPadding = insets.bottom + bottomNav.height

                view.updatePadding(bottom = bottomPadding)

                // Return insets so children (like Maps) can use them if needed
                windowInsets
            }
            // Trigger a pass to apply it immediately
            ViewCompat.requestApplyInsets(container)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = true
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        supportFragmentManager.addOnBackStackChangedListener {
            container.post {
                val current = supportFragmentManager.findFragmentById(R.id.container)
                updateHeaderVisibility(current ?: activeFragment)
            }
        }

        messageIcon.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatsFragment())
                .addToBackStack(null)
                .commit()
        }

        menuIcon.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                fun loadMenuFrag(frag: Fragment) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, frag)
                        .addToBackStack(null)
                        .commit()
                }
                when (item.itemId) {
                    R.id.action_library -> { loadMenuFrag(LibraryFragment()); true }
                    R.id.action_cafe -> { loadMenuFrag(CafeFragment()); true }
                    R.id.action_campus_map -> { loadMenuFrag(CampusMapFragment()); true }
                    R.id.action_faculty_map -> { loadMenuFrag(FacultyMapFragment()); true }
                    else -> false
                }
            }
            popup.show()
        }

        if (savedInstanceState == null) {
            feedFragment = FeedFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, feedFragment!!, "HOME")
                .commit()
            activeFragment = feedFragment
            updateHeaderVisibility(feedFragment)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (feedFragment == null) feedFragment = FeedFragment()
                    switchTab(feedFragment!!)
                    true
                }
                R.id.nav_profile -> {
                    if (profileFragment == null) profileFragment = ProfileFragment()
                    switchTab(profileFragment!!)
                    true
                }
                R.id.nav_notifications -> {
                    if (notificationFragment == null) notificationFragment = NotificationFragment()
                    switchTab(notificationFragment!!)
                    true
                }
                R.id.nav_add -> {
                    if (newPostFragment == null) newPostFragment = NewPostFragment()
                    switchTab(newPostFragment!!)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchTab(targetFragment: Fragment) {
        if (targetFragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()
        activeFragment?.let { transaction.hide(it) }

        if (targetFragment.isAdded) {
            transaction.show(targetFragment)
        } else {
            transaction.add(R.id.container, targetFragment)
        }

        transaction.commit()
        activeFragment = targetFragment
        container.post { updateHeaderVisibility(targetFragment) }
    }

    private fun updateHeaderVisibility(fragment: Fragment?) {
        val params = container.layoutParams as CoordinatorLayout.LayoutParams

        if (fragment is ProfileFragment
            || fragment is EditProfileFragment
            || fragment is CafeFragment
            || fragment is LibraryFragment
            || fragment is CampusMapFragment
            || fragment is FacultyMapFragment
            || fragment is NewPostFragment) {

            // Hide Toolbar
            appBarLayout.setExpanded(true, false)
            appBarLayout.visibility = View.GONE
            topBar.visibility = View.GONE

            // Remove scrolling behavior so content fills the screen
            params.behavior = null
        } else {
            // Show Toolbar
            appBarLayout.visibility = View.VISIBLE
            topBar.visibility = View.VISIBLE
            appBarLayout.setExpanded(true, true)

            // Re-attach behavior so content sits BELOW toolbar
            params.behavior = AppBarLayout.ScrollingViewBehavior()
        }
        container.requestLayout()
    }
}