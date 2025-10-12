package com.example.myapplication.post // Correct package name

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.myapplication.R // Correct R file import
import com.example.myapplication.home.FeedItem // Correct path to your FeedItem model
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class NewPostFragment : Fragment() {

    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var selectVideoButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var videoPreview: VideoView
    private lateinit var imagePreview: ImageView

    private var videoUri: Uri? = null
    private var imageUri: Uri? = null

    // Launcher for picking a video
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri
            imageUri = null // Ensure only one media type is selected
            videoPreview.setVideoURI(videoUri)
            videoPreview.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            videoPreview.start()
        }
    }

    // Launcher for picking an image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            videoUri = null // Ensure only one media type is selected
            imagePreview.setImageURI(imageUri)
            imagePreview.visibility = View.VISIBLE
            videoPreview.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)

        postEditText = view.findViewById(R.id.postEditText)
        postButton = view.findViewById(R.id.postButton)
        selectVideoButton = view.findViewById(R.id.selectVideoButton)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        videoPreview = view.findViewById(R.id.videoPreview)
        imagePreview = view.findViewById(R.id.imagePreview)

        selectVideoButton.setOnClickListener { pickVideoLauncher.launch("video/*") }
        selectImageButton.setOnClickListener { pickImageLauncher.launch("image/*") }

        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()
            if (content.isEmpty() && videoUri == null && imageUri == null) {
                Toast.makeText(requireContext(), "Please write something or select media!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Decide which upload path to take
            when {
                videoUri != null -> uploadVideoToCloudinary(content)
                imageUri != null -> uploadImageToFirebaseStorage(content)
                else -> savePostToFirebase(content, null, null) // Text-only post
            }
        }
        return view
    }

    // Fully implemented video upload
    private fun uploadVideoToCloudinary(postText: String) {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Uploading video...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        MediaManager.get().upload(videoUri).option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    progressDialog.dismiss()
                    val videoUrl = resultData?.get("secure_url") as String
                    savePostToFirebase(postText, null, videoUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Video upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    // Function to upload the image
    private fun uploadImageToFirebaseStorage(postText: String) {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Publishing post...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child("Post Pictures")
        val fileRef = storageRef.child(System.currentTimeMillis().toString() + ".jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    savePostToFirebase(postText, imageUrl, null)
                    progressDialog.dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }

    // Universal save function for all post types
    // In NewPostFragment.kt, replace the entire function with this updated version

    private fun savePostToFirebase(postText: String, imageUrl: String?, videoUrl: String?) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = firebaseUser.uid

        // 1. Get a reference to the current user's data in the "Users" node
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)

        // 2. Fetch the user's data ONE time to get their name
        userRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // Get the fullName we saved during sign-up
                val userName = snapshot.child("fullName").getValue(String::class.java) ?: "User"

                // 3. Now that we have the name, proceed with creating and saving the post
                val postsRef = FirebaseDatabase.getInstance().reference.child("posts")
                val postId = postsRef.push().key ?: return

                val post = FeedItem(
                    postId = postId,
                    publisher = currentUserId,
                    userName = userName, // Use the name we fetched from the database
                    postText = postText,
                    postImageUrl = imageUrl,
                    postVideoUrl = videoUrl,
                    timestamp = System.currentTimeMillis()
                )

                postsRef.child(postId).setValue(post).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Post published successfully!", Toast.LENGTH_SHORT).show()
                        // Clear fields and reset UI
                        postEditText.text.clear()
                        videoPreview.visibility = View.GONE
                        imagePreview.visibility = View.GONE
                        videoUri = null
                        imageUri = null
                    } else {
                        Toast.makeText(requireContext(), "Failed to publish post.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(requireContext(), "Could not fetch user data.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}