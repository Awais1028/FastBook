package com.example.myapplication

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Unsigned uploads: DO NOT put api_secret in the app
        val config = HashMap<String, String>()
        config["cloud_name"] = "dkzggvfnx"
        config["api_key"] = "YOUR_API_KEY" // optional for unsigned, ok to keep

        // Safe init (avoid double-init crash)
        try {
            MediaManager.get()
        } catch (e: Exception) {
            MediaManager.init(this, config)
        }
    }
}
