package com.example.myapplication.chats

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class MessageChatActivity : AppCompatActivity() {

    private var userIdVisit: String? = null
    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    private var chatsAdapter: ChatsAdapter? = null
    private val mChatList: ArrayList<Chat> = arrayListOf()
    private lateinit var recyclerViewChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_chat)

        userIdVisit = intent.getStringExtra("visit_id")
        if (userIdVisit.isNullOrEmpty() || firebaseUser == null) {
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val headerImage = findViewById<CircleImageView>(R.id.chat_profile_image)
        val headerName = findViewById<TextView>(R.id.chat_username)

        // ✅ Load header (name + profile pic)
        FirebaseDatabase.getInstance().reference
            .child("Users").child(userIdVisit!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) ?: "User"
                    val img = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                    headerName.text = name

                    if (img.isNotEmpty()) {
                        Glide.with(this@MessageChatActivity)
                            .load(img)
                            .placeholder(R.drawable.ic_profile)
                            .into(headerImage)
                    } else {
                        headerImage.setImageResource(R.drawable.ic_profile)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        recyclerViewChat = findViewById(R.id.recycler_view_messages)
        recyclerViewChat.setHasFixedSize(true)
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        recyclerViewChat.layoutManager = lm

        val sendMessageBtn = findViewById<ImageView>(R.id.send_message_btn)
        val textMessage = findViewById<EditText>(R.id.text_message)

        sendMessageBtn.setOnClickListener {
            val msg = textMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessageToUser(firebaseUser.uid, userIdVisit!!, msg)
                textMessage.setText("")
            }
        }

        retrieveMessages(firebaseUser.uid, userIdVisit!!)
    }

    private fun sendMessageToUser(senderId: String, receiverId: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val messageKey = reference.child("Chats").push().key ?: return
        val timestamp = System.currentTimeMillis()

        val messageHashMap = hashMapOf<String, Any>(
            "sender" to senderId,
            "receiver" to receiverId,
            "message" to message,
            "isseen" to false,
            "url" to "",
            "timestamp" to timestamp
        )

        reference.child("Chats").child(messageKey)
            .setValue(messageHashMap)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener

                // ✅ Update ChatList for BOTH users (for list sorting + latest msg)
                val chatListSenderRef = reference.child("ChatList").child(senderId).child(receiverId)
                val chatListReceiverRef = reference.child("ChatList").child(receiverId).child(senderId)

                val senderData = mapOf(
                    "id" to receiverId,
                    "lastMessage" to message,
                    "lastTimestamp" to timestamp
                )

                val receiverData = mapOf(
                    "id" to senderId,
                    "lastMessage" to message,
                    "lastTimestamp" to timestamp
                )

                chatListSenderRef.updateChildren(senderData)
                chatListReceiverRef.updateChildren(receiverData)
            }
    }

    private fun retrieveMessages(senderId: String, receiverId: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mChatList.clear()

                for (snap in snapshot.children) {
                    val chat = snap.getValue(Chat::class.java) ?: continue
                    val betweenUs =
                        (chat.receiver == senderId && chat.sender == receiverId) ||
                                (chat.receiver == receiverId && chat.sender == senderId)

                    if (betweenUs) mChatList.add(chat)
                }

                // ✅ Sort by timestamp (important)
                mChatList.sortBy { it.timestamp }

                chatsAdapter = ChatsAdapter(this@MessageChatActivity, mChatList)
                recyclerViewChat.adapter = chatsAdapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
