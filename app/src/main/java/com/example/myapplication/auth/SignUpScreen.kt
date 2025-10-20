package com.example.myapplication.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_screen)
        // ... your edge-to-edge code ...

        auth = FirebaseAuth.getInstance()
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etReEnterEmail = findViewById<EditText>(R.id.etReEnterEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val reEnterEmail = etReEnterEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // --- Validation ---
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email != reEnterEmail) {
                Toast.makeText(this, "Emails do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!email.endsWith("nu.edu.pk")) {
                Toast.makeText(this, "Only university emails (nu.edu.pk) are allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser

                        // --- ✅ ADD THIS BLOCK TO SEND THE VERIFICATION EMAIL ---
                        firebaseUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                // Email sent successfully
                                Toast.makeText(this, "Account created. Please check your email for a verification link.", Toast.LENGTH_LONG).show()
                            }
                            ?.addOnFailureListener { e ->
                                // Failed to send email
                                Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        // Save user info to Realtime Database
                        saveUserInfoToDatabase(fullName, email)

                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserInfoToDatabase(fullName: String, email: String) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Failed to get user ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserId
        userMap["fullName"] = fullName
        userMap["email"] = email
        userMap["profileImageUrl"] = ""

        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.child(currentUserId).setValue(userMap)
            .addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    // We finish and go back to the sign-in screen only after everything is saved.
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}