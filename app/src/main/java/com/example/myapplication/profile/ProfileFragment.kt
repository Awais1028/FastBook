package com.example.myapplication.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R

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

        // Set some dummy data
        firstName.text = "John"
        lastName.text = "Doe"
        email.text = "john.doe@example.com"

        // Example: Set the profile image from a drawable (You can load an image dynamically)
        profileImage.setImageResource(R.drawable.ic_profile)

        // Handle save button click
        saveButton.setOnClickListener {
            Toast.makeText(requireContext(), "Changes saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }
}