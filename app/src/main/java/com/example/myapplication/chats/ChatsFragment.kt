package com.example.myapplication.chats

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.UserAdapter
import com.example.myapplication.users.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatsFragment : Fragment() {
    private val lastTimeMap = HashMap<String, Long>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var search: EditText

    private val chatList = ArrayList<ChatList>()
    private val fullUserList = ArrayList<Users>()
    private val filteredUserList = ArrayList<Users>()
    private val lastMessageMap = HashMap<String, String>()

    private var userAdapter: UserAdapter? = null
    private var isFragmentActive = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_chatlist)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        search = view.findViewById(R.id.chat_search)

        view.findViewById<ImageView>(R.id.back_btn).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupSearch()

        return view
    }

    private fun loadChatList() {
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("ChatList")
            .child(myId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatList.clear()
                    lastMessageMap.clear()

                    for (s in snapshot.children) {
                        val c = s.getValue(ChatList::class.java) ?: continue
                        chatList.add(c)
                        lastMessageMap[c.id] = c.lastMessage
                        lastTimeMap[c.id] = c.lastTimestamp
                    }

                    // ✅ Sort by latest chat
                    chatList.sortByDescending { it.lastTimestamp }

                    loadUsersInChatOrder()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadUsersInChatOrder() {
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fullUserList.clear()

                    // ✅ Keep same order as chatList (already sorted)
                    for (c in chatList) {
                        val u = snapshot.child(c.id).getValue(Users::class.java)
                        if (u != null) fullUserList.add(u)
                    }

                    filteredUserList.clear()
                    filteredUserList.addAll(fullUserList)

                    if (!isFragmentActive || !isAdded) return

                    userAdapter = UserAdapter(
                        requireContext(),
                        filteredUserList,
                        true,
                        lastMessageMap,
                        lastTimeMap
                    )
                    recyclerView.adapter = userAdapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupSearch() {
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""

                filteredUserList.clear()
                if (q.isEmpty()) {
                    filteredUserList.addAll(fullUserList)
                } else {
                    filteredUserList.addAll(
                        fullUserList.filter { it.getFullName().lowercase().contains(q) }
                    )
                }

                userAdapter?.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }
    override fun onResume() {
        super.onResume()
        loadChatList() // 🔥 force refresh when coming back from chat
    }
    override fun onStart() { super.onStart(); isFragmentActive = true }
    override fun onStop() { super.onStop(); isFragmentActive = false }
}
