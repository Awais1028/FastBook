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
        val etFullName = findViewById<EditText>(R.id.etFullName) // Get the new field
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etReEnterEmail = findViewById<EditText>(R.id.etReEnterEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        // ...

        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim() // Get the full name
            val email = etEmail.text.toString().trim()
            val reEnterEmail = etReEnterEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // --- Validation ---
            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email != reEnterEmail) { /* ... */ }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { /* ... */ }
            if (!email.endsWith("nu.edu.pk")) { /* ... */ }
            if (password.length < 6) { /* ... */ }

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // --- NEW: Save user info to Realtime Database ---
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

        // Create a user map to store the data
        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserId
        userMap["fullName"] = fullName
        userMap["email"] = email
        userMap["profileImageUrl"] = "" // Add a placeholder for a profile image

        // Get a reference to the "Users" node and save the data
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.child(currentUserId).setValue(userMap)
            .addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to SignIn screen
                } else {
                    Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}