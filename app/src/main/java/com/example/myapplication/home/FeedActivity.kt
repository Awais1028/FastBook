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

    // 1. Keep references to your fragments so they aren't destroyed
    private var feedFragment: Fragment? = null
    private var profileFragment: Fragment? = null
    private var notificationFragment: Fragment? = null
    private var newPostFragment: Fragment? = null

    // Track which fragment is currently visible
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_feed)

        // Initialize Views
        appBarLayout = findViewById(R.id.appBarLayout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        topBar = findViewById(R.id.topBar)
        container = findViewById(R.id.container)
        val messageIcon = findViewById<ImageView>(R.id.messageIcon)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Padding Fixes
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }
        // 5. FIX 2: Apply BOTTOM Padding to Container
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)

            // RETURN windowInsets instead of CONSUMED.
            // This lets the Top/Status Bar info pass through to the Fragment.
            windowInsets
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = true
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Back Stack Listener (For Sub-fragments like EditProfile/Chats)
        supportFragmentManager.addOnBackStackChangedListener {
            container.post {
                val current = supportFragmentManager.findFragmentById(R.id.container)
                // If stack is empty, it means we are on a main tab (activeFragment)
                updateHeaderVisibility(current ?: activeFragment)
            }
        }

        messageIcon.setOnClickListener {
            // Chats is a sub-screen, so we use replace() to push it on top
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatsFragment())
                .addToBackStack(null)
                .commit()
        }

        menuIcon.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                // Helper to load simple menu fragments
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

        // 2. Initial Load: Load FeedFragment directly
        if (savedInstanceState == null) {
            feedFragment = FeedFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, feedFragment!!, "HOME")
                .commit()
            activeFragment = feedFragment
            updateHeaderVisibility(feedFragment)
        }

        // 3. Updated Bottom Navigation: Use Hide/Show logic
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

    // --- NEW FUNCTION: Switches tabs without destroying them ---
    private fun switchTab(targetFragment: Fragment) {
        if (targetFragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        // Hide the current fragment
        activeFragment?.let { transaction.hide(it) }

        // Show or Add the target fragment
        if (targetFragment.isAdded) {
            transaction.show(targetFragment)
        } else {
            transaction.add(R.id.container, targetFragment)
        }

        transaction.commit()
        activeFragment = targetFragment

        // Update header visibility immediately
        container.post { updateHeaderVisibility(targetFragment) }
    }

    private fun updateHeaderVisibility(fragment: Fragment?) {
        val params = container.layoutParams as CoordinatorLayout.LayoutParams

        if (fragment is ProfileFragment
            || fragment is EditProfileFragment
            || fragment is CafeFragment
            || fragment is LibraryFragment
            || fragment is CampusMapFragment
            || fragment is FacultyMapFragment) {
            appBarLayout.setExpanded(true, false)
            appBarLayout.visibility = View.GONE
            topBar.visibility = View.GONE
            params.behavior = null
        } else {
            appBarLayout.visibility = View.VISIBLE
            topBar.visibility = View.VISIBLE
            appBarLayout.setExpanded(true, true)
            params.behavior = AppBarLayout.ScrollingViewBehavior()
        }
        container.requestLayout()
    }
}