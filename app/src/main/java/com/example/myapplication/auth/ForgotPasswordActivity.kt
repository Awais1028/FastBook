package com.example.myapplication.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        // Note: Your edge-to-edge code can remain here if you wish.

        auth = FirebaseAuth.getInstance()

        val btnSendVerification = findViewById<Button>(R.id.btnSendVerification)
        val etForgotEmail = findViewById<EditText>(R.id.etForgotEmail)

        btnSendVerification.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- THIS IS THE CORE FIREBASE LOGIC ---
            // It sends a password reset link to the user's email.
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show()
                        finish() // Close this screen and go back to the sign-in page
                    } else {
                        Toast.makeText(this, "Failed to send reset link: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}