package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotpassactivityid)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnSendVerification = findViewById<Button>(R.id.btnSendVerification)
        val etForgotEmail = findViewById<EditText>(R.id.etForgotEmail)

        btnSendVerification.setOnClickListener {
            val email = etForgotEmail.text.toString()

            if (email.isNotEmpty()) {
                // Assuming you send a verification code to the email here
                // After sending, navigate to the verification code screen
                val intent = Intent(this, EnterVerificationCodeActivity::class.java)
                intent.putExtra("email", email) // Pass email to next screen
                startActivity(intent)
                finish()
            }
        }
    }
}