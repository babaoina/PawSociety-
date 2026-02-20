package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InboxActivity : BaseNavigationActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inboxAdapter: InboxAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var tabPrimary: TextView
    private lateinit var tabGeneral: TextView
    private lateinit var tabRequests: TextView
    private lateinit var tabIndicator: View
    private lateinit var tvUsername: TextView

    private var currentTab = "primary"
    private var allUsers = listOf<AppUser>()
    private var currentUser: AppUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        currentUser = UserDatabase.getCurrentUser(this)
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view inbox", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        loadOtherUsers()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.inbox_recycler_view)
        emptyState = findViewById(R.id.empty_state)
        tabPrimary = findViewById(R.id.tab_primary)
        tabGeneral = findViewById(R.id.tab_general)
        tabRequests = findViewById(R.id.tab_requests)
        tabIndicator = findViewById(R.id.tab_indicator)
        tvUsername = findViewById(R.id.tv_username)

        tvUsername.text = currentUser?.username ?: "username"

        recyclerView.layoutManager = LinearLayoutManager(this)

        updateTabSelection("primary")
    }

    private fun setupClickListeners() {
        tabPrimary.setOnClickListener {
            updateTabSelection("primary")
            loadPrimaryUsers()
        }

        tabGeneral.setOnClickListener {
            updateTabSelection("general")
            loadGeneralUsers()
        }

        tabRequests.setOnClickListener {
            updateTabSelection("requests")
            loadRequestsUsers()
        }

        val searchInput = findViewById<EditText>(R.id.search_input)
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            if (::inboxAdapter.isInitialized) {
                inboxAdapter.notifyDataSetChanged()
            }
        } else {
            val filteredUsers = allUsers.filter {
                it.username.contains(query, ignoreCase = true)
            }

            if (filteredUsers.isEmpty()) {
                Toast.makeText(this, "No users found matching '$query'", Toast.LENGTH_SHORT).show()
            } else {
                inboxAdapter = InboxAdapter(filteredUsers) { user ->
                    openConversation(user)
                }
                recyclerView.adapter = inboxAdapter
            }
        }
    }

    private fun updateTabSelection(tab: String) {
        currentTab = tab

        tabPrimary.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        tabGeneral.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        tabRequests.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        when (tab) {
            "primary" -> {
                tabPrimary.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tabIndicator.animate().x(tabPrimary.x).setDuration(200).start()
            }
            "general" -> {
                tabGeneral.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tabIndicator.animate().x(tabGeneral.x).setDuration(200).start()
            }
            "requests" -> {
                tabRequests.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tabIndicator.animate().x(tabRequests.x).setDuration(200).start()
            }
        }
    }

    private fun loadOtherUsers() {
        allUsers = UserDatabase.getOtherUsers(this)

        if (allUsers.isEmpty()) {
            showEmptyState()
        } else {
            showUserList()
            setupAdapter(allUsers)
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE

        val emptyText = findViewById<TextView>(R.id.empty_text)
        emptyText.text = "No other users yet\n\nWhen other people register, they will appear here"
    }

    private fun showUserList() {
        recyclerView.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    private fun setupAdapter(users: List<AppUser>) {
        inboxAdapter = InboxAdapter(users) { user ->
            openConversation(user)
        }
        recyclerView.adapter = inboxAdapter
    }

    private fun loadPrimaryUsers() {
        if (::inboxAdapter.isInitialized) {
            inboxAdapter.notifyDataSetChanged()
        }
    }

    private fun loadGeneralUsers() {
        Toast.makeText(this, "No general messages", Toast.LENGTH_SHORT).show()
    }

    private fun loadRequestsUsers() {
        Toast.makeText(this, "No message requests", Toast.LENGTH_SHORT).show()
    }

    private fun openConversation(user: AppUser) {
        Toast.makeText(this, "Opening chat with @${user.username}", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadOtherUsers()
    }
}