package com.example.myapplication.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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
import com.example.myapplication.profile.ProfileFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedActivity : AppCompatActivity() {

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var topBar: View
    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    private var feedFragment: FeedFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var notificationFragment: NotificationFragment? = null
    private var newPostFragment: NewPostFragment? = null
    private var chatsFragment: ChatsFragment? = null

    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_feed)

        appBarLayout = findViewById(R.id.appBarLayout)
        topBar = findViewById(R.id.topBar)
        container = findViewById(R.id.container)
        bottomNav = findViewById(R.id.bottom_navigation)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Status bar padding
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top)
            WindowInsetsCompat.CONSUMED
        }

        // Bottom padding
        bottomNav.post {
            ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
                val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(bottom = sys.bottom + bottomNav.height)
                insets
            }
            ViewCompat.requestApplyInsets(container)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val visibleFragment = supportFragmentManager.fragments
                .lastOrNull { it != null && it.isVisible }

            updateHeaderVisibility(visibleFragment)
        }

        ViewCompat.getWindowInsetsController(window.decorView)?.let {
            it.isAppearanceLightStatusBars = true
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Default fragment
        if (savedInstanceState == null) {
            feedFragment = FeedFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, feedFragment!!, "HOME")
                .commit()
            activeFragment = feedFragment
            updateHeaderVisibility(feedFragment)
        }

        // Bottom navigation
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

                R.id.nav_chats -> {
                    if (chatsFragment == null) chatsFragment = ChatsFragment()
                    switchTab(chatsFragment!!)
                    true
                }

                else -> false
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val visible = supportFragmentManager.fragments.lastOrNull { it.isVisible }
            activeFragment = visible ?: activeFragment
            updateHeaderVisibility(activeFragment)
        }

        fun openOverlayFragment(fragment: Fragment) {
            val tx = supportFragmentManager.beginTransaction()
            activeFragment?.let { tx.hide(it) }
            tx.add(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Search (Feed only)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                (activeFragment as? FeedFragment)
                    ?.onSearchQuery(s?.toString()?.trim().orEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Menu (3-dot)
        menuIcon.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                val fragment = when (item.itemId) {
                    R.id.action_library -> LibraryFragment()
                    R.id.action_cafe -> CafeFragment()
                    R.id.action_campus_map -> CampusMapFragment()
                    R.id.action_faculty_map -> FacultyMapFragment()
                    else -> null
                } ?: return@setOnMenuItemClickListener false

                // ✅ IMPORTANT: hide whatever is currently visible (your active tab)
                val current = getVisibleFragment()

                supportFragmentManager.beginTransaction().apply {
                    if (current != null) hide(current)
                    add(R.id.container, fragment)
                    addToBackStack(null)
                    commit()
                }

                // ✅ hide top+bottom for menu screens
                updateHeaderVisibility(fragment)

                true
            }

            popup.show()
        }


    }
    fun openPostDetail(postId: String) {
        val fragment = PostDetailFragment().apply {
            arguments = Bundle().apply { putString("postId", postId) }
        }

        // IMPORTANT: don't "replace" because it removes your existing tab fragments
        supportFragmentManager.beginTransaction()
            .hide(activeFragment ?: return)
            .add(R.id.container, fragment, "POST_DETAIL")
            .addToBackStack("POST_DETAIL")
            .commit()

        // On detail screen: no header/bottom
        updateHeaderVisibility(fragment)
    }

    fun openHomeTab() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun getVisibleFragment(): Fragment? {
        return supportFragmentManager.fragments.lastOrNull { it != null && it.isVisible }
    }

    private fun switchTab(target: Fragment) {
        if (target == activeFragment) return

        val tx = supportFragmentManager.beginTransaction()
        activeFragment?.let { tx.hide(it) }

        if (target.isAdded) tx.show(target)
        else tx.add(R.id.container, target)

        tx.commit()
        activeFragment = target
        updateHeaderVisibility(target)
    }

    private fun updateHeaderVisibility(fragment: Fragment?) {
        if (fragment == null) return   // ✅ CRASH FIX

        val params = container.layoutParams as CoordinatorLayout.LayoutParams

        when (fragment) {
            is FeedFragment -> {
                appBarLayout.visibility = View.VISIBLE
                topBar.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
                params.behavior = AppBarLayout.ScrollingViewBehavior()
            }

            is ProfileFragment,
            is NewPostFragment,
            is NotificationFragment,
            is ChatsFragment -> {
                appBarLayout.visibility = View.GONE
                topBar.visibility = View.GONE
                bottomNav.visibility = View.VISIBLE
                params.behavior = null
            }

            else -> {
                appBarLayout.visibility = View.GONE
                topBar.visibility = View.GONE
                bottomNav.visibility = View.GONE
                params.behavior = null
            }
        }

        container.requestLayout()
    }
}
