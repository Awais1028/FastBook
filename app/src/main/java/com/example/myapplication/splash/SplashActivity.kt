package com.example.myapplication.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.example.myapplication.R
import com.example.myapplication.auth.SignInScreen
import com.example.myapplication.home.FeedActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen_second_layout)

        Handler(Looper.getMainLooper()).postDelayed({

            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                // ✅ User already logged in
                startActivity(Intent(this, FeedActivity::class.java))
            } else {
                // ❌ Not logged in
                startActivity(Intent(this, SignInScreen::class.java))
            }

            finish()

        }, 2000) // 2 seconds is enough (5s feels long)
    }
}
