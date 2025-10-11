package com.example.myapplication.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changepasswordactivityid)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etRetypePassword = findViewById<EditText>(R.id.etRetypePassword)

        btnChangePassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val retypePassword = etRetypePassword.text.toString()

            if (newPassword == retypePassword && newPassword.isNotEmpty()) {
                finish()
                // Perform password change logic here, and then confirm success
                // Navigate to sign-in screen or show success message
            } else {
                Toast.makeText(applicationContext, "Passwords don't match", Toast.LENGTH_SHORT).show()
                // Show error (e.g., passwords don't match)
            }
        }
    }
}