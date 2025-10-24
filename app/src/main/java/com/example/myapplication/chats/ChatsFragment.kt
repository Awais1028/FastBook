package com.example.myapplication.chats // Or your correct package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.UserAdapter // Make sure you have this adapter
import com.example.myapplication.chats.ChatList
import com.example.myapplication.users.Users // Make sure you have these models
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatsFragment : Fragment() {

    private var userAdapter: UserAdapter? = null
    private var userList: List<Users>? = null
    private var usersChatList: List<ChatList>? = null
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_chatlist)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        usersChatList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("ChatList").child(firebaseUser!!.uid)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (usersChatList as ArrayList).clear()
                for (dataSnapshot in snapshot.children) {
                    val chatList = dataSnapshot.getValue(ChatList::class.java)
                    (usersChatList as ArrayList).add(chatList!!)
                }
                retrieveChatList()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }

    private fun retrieveChatList() {
        userList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (userList as ArrayList).clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(Users::class.java)
                    for (eachChatList in usersChatList!!) {
                        if (user!!.getUID() == eachChatList.getId()) {
                            (userList as ArrayList).add(user)
                        }
                    }
                }
                userAdapter = UserAdapter(context!!, (userList as ArrayList<Users>), true)
                recyclerView.adapter = userAdapter
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}