package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R

class EnterVerificationCodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter_verification_code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.enterverificationcodeactivityid)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val email = intent.getStringExtra("email") // Retrieve passed email
        val btnVerifyCode = findViewById<Button>(R.id.btnVerifyCode)
        val etVerificationCode = findViewById<EditText>(R.id.etVerificationCode)
        val tvDidNotReceiveCode = findViewById<TextView>(R.id.tvDidNotReceiveCode)

        btnVerifyCode.setOnClickListener {
            val verificationCode = etVerificationCode.text.toString()

            if (verificationCode.isNotEmpty()) {
                // Here, verify the code (for simplicity, assuming success)
                // After verification, navigate to the change password screen
                val intent = Intent(this, ChangePasswordActivity::class.java)
                intent.putExtra("email", email) // Pass email to next screen
                startActivity(intent)
                finish()
            }
        }

        tvDidNotReceiveCode.setOnClickListener {
            // If the user didn't receive the code, you can re-send the code logic here
            // Or show a message, etc.
        }
    }
}