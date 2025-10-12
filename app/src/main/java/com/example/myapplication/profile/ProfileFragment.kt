package com.example.myapplication.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profileImage: ImageView
    private lateinit var firstName: TextView
    private lateinit var lastName: TextView
    private lateinit var email: TextView
    private lateinit var saveButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        profileImage = view.findViewById(R.id.profileImage)
        firstName = view.findViewById(R.id.editFirstName)
        lastName = view.findViewById(R.id.editLastName)
        email = view.findViewById(R.id.editEmail)
        saveButton = view.findViewById(R.id.saveButton)

        // --- NEW: Load user data from Firebase ---
        loadUserInfo()

        // You can add logic here for what happens when the user saves changes
        saveButton.setOnClickListener {
            Toast.makeText(requireContext(), "Save functionality not implemented yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserInfo() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userFullName = snapshot.child("fullName").getValue(String::class.java)
                    val userEmail = snapshot.child("email").getValue(String::class.java)

                    // Set the retrieved data to the TextViews
                    email.text = userEmail

                    // Split the full name into first and last names
                    val names = userFullName?.split(" ")
                    if (names != null && names.isNotEmpty()) {
                        firstName.text = names[0]
                        lastName.text = if (names.size > 1) names.last() else ""
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}