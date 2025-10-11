package com.example.myapplication.post

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.myapplication.R

class NewPostFragment : Fragment() {

    private lateinit var postEditText: EditText
    private lateinit var postButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)

        postEditText = view.findViewById(R.id.postEditText)
        postButton = view.findViewById(R.id.postButton)

        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()
            if (content.isNotEmpty()) {
                // Handle sending post content to backend or storing in ViewModel
                Toast.makeText(requireContext(), "Post added: $content", Toast.LENGTH_SHORT).show()
                postEditText.text.clear()
                // Optionally navigate back or update Feed
            } else {
                Toast.makeText(requireContext(), "Please write something!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
    fun uploadVideoToCloudinary(videoUri: Uri) {
        MediaManager.get().upload(videoUri).option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Show a progress bar or loading spinner
                }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Update progress bar
                }
                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    // Get the video URL from the result
                    val videoUrl = resultData?.get("url") as String

                    // Now, you can save this videoUrl to Firebase
                    // and create a new post with this URL.
                    // Call your createPost function here with the videoUrl
                    // createPost(postText, null, videoUrl)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    // Show an error message to the user
                    Toast.makeText(requireContext(), "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Handle reschedules
                }
            }).dispatch()
    }
}