package com.example.myapplication.chats // Or your correct package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView

class ChatsFragment : Fragment() {
    private var isFragmentActive = false
    private var fullUserList = ArrayList<Users>()
    private var filteredUserList = ArrayList<Users>()

    private var userAdapter: UserAdapter? = null
    private val lastMessageMap = HashMap<String, String>()

    private var userList: List<Users>? = null
    private var usersChatList: List<ChatList>? = null
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top)   // fixes "eaten by status bar"
            insets
        }
        ViewCompat.requestApplyInsets(view)

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
        view.findViewById<ImageView>(R.id.back_btn).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        val search = view.findViewById<EditText>(R.id.chat_search)

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                val filtered = (userList as ArrayList<Users>).filter {
                    it.getFullName().lowercase().contains(q)
                }
                search.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val q = s?.toString()?.trim()?.lowercase() ?: ""

                        filteredUserList.clear()

                        if (q.isEmpty()) {
                            filteredUserList.addAll(fullUserList)
                        } else {
                            filteredUserList.addAll(
                                fullUserList.filter {
                                    it.getFullName().lowercase().contains(q)
                                }
                            )
                        }

                        userAdapter?.notifyDataSetChanged()
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    private fun retrieveChatList() {
        userList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullUserList.clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(Users::class.java)
                    for (eachChatList in usersChatList!!) {
                        if (user!!.getUID() == eachChatList.getId()) {
                            fullUserList.add(user)
                        }
                    }
                }
                filteredUserList.clear()
                filteredUserList.addAll(fullUserList)
                fetchLastMessagesAndSetAdapter(filteredUserList)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun fetchLastMessagesAndSetAdapter(users: List<Users>) {
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("Chats")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastMessageMap.clear()

                // We'll keep the latest message we see for each user
                for (s in snapshot.children) {
                    val chat = s.getValue(Chat::class.java) ?: continue

                    val otherUserId =
                        if (chat.sender == myId) chat.receiver
                        else if (chat.receiver == myId) chat.sender
                        else null

                    if (otherUserId != null) {
                        lastMessageMap[otherUserId] = chat.message
                    }
                }

                if (!isFragmentActive || !isAdded || context == null) return

                userAdapter = UserAdapter(
                    context!!,
                    filteredUserList,
                    true,
                    lastMessageMap
                )
                recyclerView.adapter = userAdapter

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    override fun onStart() {
        super.onStart()
        isFragmentActive = true
    }
    override fun onStop() {
        super.onStop()
        isFragmentActive = false
    }


}