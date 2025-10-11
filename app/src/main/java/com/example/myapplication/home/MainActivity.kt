package com.example.myapplication.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.auth.SignInScreen
import com.google.firebase.FirebaseApp
import com.cloudinary.android.MediaManager
import java.util.HashMap

// In your onCreate() method


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val isLoggedIn = false // Replace with your real logic
        val config: HashMap<String, String> = HashMap()
        config["cloud_name"] = "dkzggvfnx"
        config["api_key"] = "149356222841545"
        config["api_secret"] = "PcwhvCv-03vauBF02kPN9jzVapU"
        MediaManager.init(this, config)
        if (!isLoggedIn) {
            val intent = Intent(this, SignInScreen::class.java)
            startActivity(intent)
            finish() // Optional: prevent back navigation to MainActivity
        } else {
            setContent {
                HomeScreen()
            }
        }
        setContent {
            HomeScreen()
        }

    }
}


@Composable
fun HomeScreen() {
    Text("Welcome to Fastians App!")
}

@Preview
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}