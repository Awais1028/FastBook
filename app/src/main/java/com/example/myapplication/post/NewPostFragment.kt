package com.example.myapplication.post

import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.myapplication.R
import com.example.myapplication.home.FeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NewPostFragment : Fragment() {

    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var selectVideoButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var videoPreview: VideoView
    private lateinit var imagePreview: ImageView
    private lateinit var categorySpinner: Spinner

    private var videoUri: Uri? = null
    private var imageUri: Uri? = null
    private var mediaWidth: Int = 0
    private var mediaHeight: Int = 0

    // ✅ your Cloudinary unsigned preset name:
    private val UPLOAD_PRESET = "fastians_unsigned"
    private val UPLOAD_FOLDER = "fastians/posts"

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                videoUri = uri
                imageUri = null
                getImageDimensions(Uri.EMPTY)
                getVideoDimensions(uri)

                view?.findViewById<View>(R.id.preview_container)?.visibility = View.VISIBLE
                videoPreview.visibility = View.VISIBLE
                imagePreview.visibility = View.GONE

                videoPreview.setVideoURI(videoUri)
                videoPreview.start()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageUri = uri
                videoUri = null
                getVideoDimensions(Uri.EMPTY)
                getImageDimensions(uri)

                view?.findViewById<View>(R.id.preview_container)?.visibility = View.VISIBLE
                imagePreview.setImageURI(imageUri)
                imagePreview.visibility = View.VISIBLE
                videoPreview.visibility = View.GONE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_new_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }

        postEditText = view.findViewById(R.id.postEditText)
        postButton = view.findViewById(R.id.postButton)
        selectVideoButton = view.findViewById(R.id.selectVideoButton)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        videoPreview = view.findViewById(R.id.videoPreview)
        imagePreview = view.findViewById(R.id.imagePreview)
        categorySpinner = view.findViewById(R.id.category_spinner)

        val categories = arrayOf("General", "Education", "Sports", "Cafe-Food", "Events", "Memes", "Tech")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        selectVideoButton.setOnClickListener { pickVideoLauncher.launch("video/*") }
        selectImageButton.setOnClickListener { pickImageLauncher.launch("image/*") }

        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()
            val selectedCategory = categorySpinner.selectedItem.toString()

            if (content.isEmpty() && videoUri == null && imageUri == null) {
                Toast.makeText(requireContext(), "Please write something or select media!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postButton.isEnabled = false

            when {
                videoUri != null -> uploadVideoToCloudinary(content, selectedCategory)
                imageUri != null -> uploadImageToCloudinary(content, selectedCategory)
                else -> savePostToFirebase(content, selectedCategory, null, null, 0, 0)
            }
        }
    }

    private fun uploadImageToCloudinary(postText: String, category: String) {
        val uri = imageUri
        if (uri == null) {
            postButton.isEnabled = true
            Toast.makeText(requireContext(), "No image selected.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading image...")
            setCancelable(false)
            show()
        }

        MediaManager.get().upload(uri)
            .unsigned(UPLOAD_PRESET)                 // ✅ unsigned upload
            .option("folder", UPLOAD_FOLDER)
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    progressDialog.dismiss()
                    val imageUrl = resultData?.get("secure_url")?.toString()
                    if (imageUrl.isNullOrEmpty()) {
                        postButton.isEnabled = true
                        Toast.makeText(requireContext(), "Upload returned empty URL.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    savePostToFirebase(postText, category, imageUrl, null, mediaWidth, mediaHeight)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    progressDialog.dismiss()
                    postButton.isEnabled = true
                    Toast.makeText(requireContext(), "Image upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // if it reschedules, keep button disabled until it finishes or fails
                }
            })
            .dispatch()
    }

    private fun uploadVideoToCloudinary(postText: String, category: String) {
        val uri = videoUri
        if (uri == null) {
            postButton.isEnabled = true
            Toast.makeText(requireContext(), "No video selected.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading video...")
            setCancelable(false)
            show()
        }

        MediaManager.get().upload(uri)
            .unsigned(UPLOAD_PRESET)                 // ✅ unsigned upload
            .option("folder", UPLOAD_FOLDER)
            .option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    progressDialog.dismiss()
                    val videoUrl = resultData?.get("secure_url")?.toString()
                    if (videoUrl.isNullOrEmpty()) {
                        postButton.isEnabled = true
                        Toast.makeText(requireContext(), "Upload returned empty URL.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    savePostToFirebase(postText, category, null, videoUrl, mediaWidth, mediaHeight)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    progressDialog.dismiss()
                    postButton.isEnabled = true
                    Toast.makeText(requireContext(), "Video upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun savePostToFirebase(
        postText: String,
        category: String,
        imageUrl: String?,
        videoUrl: String?,
        width: Int,
        height: Int
    ) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            postButton.isEnabled = true
            return
        }

        val currentUserId = firebaseUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("fullName").getValue(String::class.java) ?: "User"
                val postsRef = FirebaseDatabase.getInstance().getReference("posts")
                val postId = postsRef.push().key ?: run {
                    postButton.isEnabled = true
                    return
                }

                val post = FeedItem(
                    postId = postId,
                    publisher = currentUserId,
                    userName = userName,
                    postText = postText,
                    postImageUrl = imageUrl,
                    postVideoUrl = videoUrl,
                    timestamp = System.currentTimeMillis(),
                    mediaWidth = width,
                    mediaHeight = height,
                    category = category
                )

                postsRef.child(postId).setValue(post).addOnCompleteListener { task ->
                    postButton.isEnabled = true
                    if (task.isSuccessful) {
                        resetForm()
                        Toast.makeText(requireContext(), "Post published successfully!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult("feed_refresh", Bundle())
                        (activity as? com.example.myapplication.home.FeedActivity)?.openHomeTab()

                    } else {
                        Toast.makeText(requireContext(), "Failed to publish post.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                postButton.isEnabled = true
            }
        })
    }

    private fun getImageDimensions(uri: Uri) {
        if (uri == Uri.EMPTY) { mediaWidth = 0; mediaHeight = 0; return }
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            requireActivity().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            mediaWidth = options.outWidth
            mediaHeight = options.outHeight
        } catch (e: Exception) {
            Log.e("NewPostFragment", "getImageDimensions error", e)
        }
    }

    private fun getVideoDimensions(uri: Uri) {
        if (uri == Uri.EMPTY) { mediaWidth = 0; mediaHeight = 0; return }
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), uri)
            mediaWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            mediaHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.release()
        } catch (e: Exception) {
            Log.e("NewPostFragment", "getVideoDimensions error", e)
        }
    }
    private fun resetForm() {
        postEditText.setText("")
        categorySpinner.setSelection(0)

        imageUri = null
        videoUri = null
        mediaWidth = 0
        mediaHeight = 0

        // Stop video and hide preview
        videoPreview.stopPlayback()
        videoPreview.setVideoURI(null)

        imagePreview.setImageDrawable(null)

        view?.findViewById<View>(R.id.preview_container)?.visibility = View.GONE
        videoPreview.visibility = View.GONE
        imagePreview.visibility = View.GONE
    }

}
