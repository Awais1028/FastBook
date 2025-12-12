package com.example.myapplication.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.auth.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = false // replace with real logic

        if (!isLoggedIn) {
            startActivity(Intent(this, SignInScreen::class.java))
            finish()
            return
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
