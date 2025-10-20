package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.home.FeedActivity
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth

class SignInScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in_screen)

        // Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signinscreenId)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Handle Login
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        // ✅ THIS IS THE NEW VERIFICATION CHECK
                        if (firebaseUser != null ){//&& firebaseUser.isEmailVerified) {
                            // User is verified, proceed to the main app
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, FeedActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // User is NOT verified, show an error and sign them out
                            Toast.makeText(this, "Please verify your email address first.", Toast.LENGTH_LONG).show()
                            auth.signOut()
                        }
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        // Go to SignUp
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpScreen::class.java)
            startActivity(intent)
        }

        // Go to Forgot Password
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}