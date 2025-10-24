package com.example.myapplication.chats

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.chats.ChatsAdapter
import com.example.myapplication.chats.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessageChatActivity : AppCompatActivity() {

    var userIdVisit: String?    = null
    var firebaseUser = FirebaseAuth.getInstance().currentUser

    private var chatsAdapter: ChatsAdapter? = null
    private var mChatList: List<Chat>? = null
    lateinit var recyclerViewChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_chat)

        userIdVisit = intent.getStringExtra("visit_id")

        val toolbar: Toolbar = findViewById(R.id.toolbar_chat)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "" // We can set the username here later
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerViewChat = findViewById(R.id.recycler_view_messages)
        recyclerViewChat.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = linearLayoutManager

        val sendMessageBtn = findViewById<ImageView>(R.id.send_message_btn)
        val textMessage = findViewById<EditText>(R.id.text_message)

        sendMessageBtn.setOnClickListener {
            val message = textMessage.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToUser(firebaseUser!!.uid, userIdVisit!!, message)
            }
            textMessage.setText("")
        }

        // Load chat history
        retrieveMessages(firebaseUser!!.uid, userIdVisit!!)
    }

    private fun sendMessageToUser(senderId: String, receiverId: String, message: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val messageKey = reference.push().key

        val messageHashMap = HashMap<String, Any>()
        messageHashMap["sender"] = senderId
        messageHashMap["receiver"] = receiverId
        messageHashMap["message"] = message
        messageHashMap["isseen"] = false
        messageHashMap["url"] = ""

        reference.child("Chats").child(messageKey!!).setValue(messageHashMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val chatListRef = FirebaseDatabase.getInstance()
                        .reference.child("ChatList").child(firebaseUser!!.uid).child(userIdVisit!!)
                    chatListRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                chatListRef.child("id").setValue(userIdVisit)
                            }
                            val chatListReceiverRef = FirebaseDatabase.getInstance()
                                .reference.child("ChatList").child(userIdVisit!!).child(firebaseUser!!.uid)
                            chatListReceiverRef.child("id").setValue(firebaseUser!!.uid)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
    }

    private fun retrieveMessages(senderId: String, receiverId: String) {
        mChatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (mChatList as ArrayList<Chat>).clear()
                for (snap in snapshot.children) {
                    val chat = snap.getValue(Chat::class.java)
                    if (chat!!.receiver == senderId && chat.sender == receiverId ||
                        chat.receiver == receiverId && chat.sender == senderId
                    ) {
                        (mChatList as ArrayList<Chat>).add(chat)
                    }
                }
                chatsAdapter = ChatsAdapter(this@MessageChatActivity, (mChatList as ArrayList<Chat>))
                recyclerViewChat.adapter = chatsAdapter
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}