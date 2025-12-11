package com.example.myapplication.profile

import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.signature.ObjectKey // 👈 ADD THIS IMPORT
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import com.cloudinary.android.MediaManager // Import Cloudinary
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.myapplication.home.FeedActivity

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private lateinit var profileImage: CircleImageView
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar

    private var imageUri: Uri? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var userRef: DatabaseReference

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            profileImage.setImageURI(imageUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        profileImage = view.findViewById(R.id.profileImage)
        firstName = view.findViewById(R.id.editFirstName)
        lastName = view.findViewById(R.id.editLastName)
        email = view.findViewById(R.id.editEmail)
        saveButton = view.findViewById(R.id.saveButton)
        progressBar = view.findViewById(R.id.progressBar)

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId!!)

        loadUserInfo()

        profileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadUserInfo() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    val userFullName = snapshot.child("fullName").getValue(String::class.java)
                    val userEmail = snapshot.child("email").getValue(String::class.java)
                    val profilePicUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    email.setText(userEmail)

                    val names = userFullName?.split(" ")
                    if (names != null && names.isNotEmpty()) {
                        firstName.setText(names.first())
                        lastName.setText(if (names.size > 1) names.last() else "")
                    }

                    if (!profilePicUrl.isNullOrEmpty()) {
                        Glide.with(this@EditProfileFragment).load(profilePicUrl).into(profileImage)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveChanges() {
        progressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false

        if (imageUri != null) {
            // If a new image was selected, upload it first using Cloudinary
            uploadImageWithCloudinary()
        } else {
            // If only text was changed, just update the user info
            updateUserInfo(null)
        }
    }

    // --- ✅ THIS IS THE NEW, RELIABLE CLOUDINARY UPLOAD LOGIC ---
    private fun uploadImageWithCloudinary() {
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val newImageUrl = resultData["secure_url"].toString()
                    // After getting the URL, update all user info
                    updateUserInfo(newImageUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Toast.makeText(context, "Image upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun updateUserInfo(newImageUrl: String?) {
        val newFirstName = firstName.text.toString().trim()
        val newLastName = lastName.text.toString().trim()
        val newFullName = "$newFirstName $newLastName".trim()

        val updates = mutableMapOf<String, Any>()
        updates["fullName"] = newFullName
        if (newImageUrl != null) {
            updates["profileImageUrl"] = newImageUrl
        }

        userRef.updateChildren(updates).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            saveButton.isEnabled = true
            if (task.isSuccessful) {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Go back to the previous screen
            } else {
                Toast.makeText(context, "Failed to update profile.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}