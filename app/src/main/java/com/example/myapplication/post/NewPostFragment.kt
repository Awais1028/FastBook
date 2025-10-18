package com.example.myapplication.post // Correct package name

import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log // Added for debugging

class NewPostFragment : Fragment() {

    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var selectVideoButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var videoPreview: VideoView
    private lateinit var imagePreview: ImageView

    private var videoUri: Uri? = null
    private var imageUri: Uri? = null
    private var mediaWidth: Int = 0
    private var mediaHeight: Int = 0

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri
            imageUri = null // Ensure only one media type is selected
            getImageDimensions(Uri.EMPTY) // Clear image dimensions
            getVideoDimensions(uri) // Get video dimensions
            videoPreview.setVideoURI(videoUri)
            videoPreview.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            videoPreview.start()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            videoUri = null // Ensure only one media type is selected
            getVideoDimensions(Uri.EMPTY) // Clear video dimensions
            getImageDimensions(uri) // Get image dimensions
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
                else -> savePostToFirebase(content, null, null, 0, 0) // Text-only post
            }
        }
        return view
    }

    private fun getImageDimensions(uri: Uri) {
        if (uri == Uri.EMPTY) {
            mediaWidth = 0
            mediaHeight = 0
            return
        }
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            requireActivity().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            mediaWidth = options.outWidth
            mediaHeight = options.outHeight
            Log.d("NewPostFragment", "Image dimensions: $mediaWidth x $mediaHeight")
        } catch (e: Exception) {
            Log.e("NewPostFragment", "Error getting image dimensions: ${e.message}", e)
            mediaWidth = 0
            mediaHeight = 0
        }
    }

    private fun getVideoDimensions(uri: Uri) {
        if (uri == Uri.EMPTY) {
            mediaWidth = 0
            mediaHeight = 0
            return
        }
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), uri)
            mediaWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            mediaHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.release()
            Log.d("NewPostFragment", "Video dimensions: $mediaWidth x $mediaHeight")
        } catch (e: Exception) {
            Log.e("NewPostFragment", "Error getting video dimensions: ${e.message}", e)
            mediaWidth = 0
            mediaHeight = 0
        }
    }

    private fun uploadVideoToCloudinary(postText: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading video...")
            setCancelable(false)
            show()
        }

        MediaManager.get().upload(videoUri).option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    progressDialog.dismiss()
                    val videoUrl = resultData?.get("secure_url") as String
                    savePostToFirebase(postText, null, videoUrl, mediaWidth, mediaHeight)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Video upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onStart(requestId: String) { Log.d("NewPostFragment", "Cloudinary upload started: $requestId") }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { Log.d("NewPostFragment", "Cloudinary progress: $bytes / $totalBytes") }
                override fun onReschedule(requestId: String, error: ErrorInfo) { Log.d("NewPostFragment", "Cloudinary reschedule: $requestId - ${error.description}") }
            }).dispatch()
    }

    private fun uploadImageToFirebaseStorage(postText: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Publishing post...")
            setCancelable(false)
            show()
        }

        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child("Post Pictures")
        val fileRef = storageRef.child("${System.currentTimeMillis()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    progressDialog.dismiss()
                    val imageUrl = uri.toString()
                    savePostToFirebase(postText, imageUrl, null, mediaWidth, mediaHeight)
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("NewPostFragment", "Firebase image upload failed", e)
            }
    }

    private fun savePostToFirebase(postText: String, imageUrl: String?, videoUrl: String?, width: Int, height: Int) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = firebaseUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("fullName").getValue(String::class.java) ?: "User"
                val postsRef = FirebaseDatabase.getInstance().getReference("posts") // Correct lowercase "posts" path
                val postId = postsRef.push().key ?: return

                val post = FeedItem(
                    postId = postId,
                    publisher = currentUserId,
                    userName = userName,
                    postText = postText,
                    postImageUrl = imageUrl,
                    postVideoUrl = videoUrl,
                    timestamp = System.currentTimeMillis(),
                    mediaWidth = width,
                    mediaHeight = height
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
                        mediaWidth = 0
                        mediaHeight = 0
                    } else {
                        Toast.makeText(requireContext(), "Failed to publish post.", Toast.LENGTH_SHORT).show()
                        Log.e("NewPostFragment", "Failed to save post to Firebase", task.exception)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Could not fetch user data for post.", Toast.LENGTH_SHORT).show()
                Log.e("NewPostFragment", "Firebase user data fetch cancelled: ${error.message}")
            }
        })
    }
}