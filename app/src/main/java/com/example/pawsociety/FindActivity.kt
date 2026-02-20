package com.example.pawsociety

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FindActivity : BaseNavigationActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var btnFilter: LinearLayout
    private lateinit var chipAll: TextView
    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView
    private lateinit var chipAdoption: TextView

    private var currentFilter = "All"
    private var allPosts = listOf<Post>()
    private var filteredPosts = listOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find)

        // Check if user is logged in
        val currentUser = UserDatabase.getCurrentUser(this)
        if (currentUser == null) {
            Toast.makeText(this, "Please login to find pets", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            initViews()
            setupClickListeners()
            loadPosts()
            // Set initial highlight on All Pets
            highlightChip(chipAll)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.find_recycler_view)
        emptyState = findViewById(R.id.empty_state)
        searchInput = findViewById(R.id.search_input)
        btnFilter = findViewById(R.id.btn_filter)
        chipAll = findViewById(R.id.chip_all)
        chipLost = findViewById(R.id.chip_lost)
        chipFound = findViewById(R.id.chip_found)
        chipAdoption = findViewById(R.id.chip_adoption)

        // Setup RecyclerView with 3 columns and spacing
        val gridLayoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager

        // Add item decoration for spacing
        val spacing = 4
        recyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacing, true))
    }

    private fun setupClickListeners() {
        // Filter button click
        btnFilter.setOnClickListener {
            Toast.makeText(this, "Filter options coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Search input
        searchInput.setOnEditorActionListener { _, _, _ ->
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
            true
        }

        // Filter chips - ONLY the selected one gets highlighted
        chipAll.setOnClickListener {
            if (currentFilter != "All") {
                currentFilter = "All"
                highlightChip(chipAll)
                filterPosts()
            }
        }

        chipLost.setOnClickListener {
            if (currentFilter != "Lost") {
                currentFilter = "Lost"
                highlightChip(chipLost)
                filterPosts()
            }
        }

        chipFound.setOnClickListener {
            if (currentFilter != "Found") {
                currentFilter = "Found"
                highlightChip(chipFound)
                filterPosts()
            }
        }

        chipAdoption.setOnClickListener {
            if (currentFilter != "Adoption") {
                currentFilter = "Adoption"
                highlightChip(chipAdoption)
                filterPosts()
            }
        }
    }

    private fun highlightChip(selectedChip: TextView) {
        // Reset all chips to default colors (no highlight)
        resetAllChips()

        // Highlight the selected chip by making it fully opaque and bold
        selectedChip.alpha = 1.0f
        selectedChip.setTextColor(Color.WHITE)
    }

    private fun resetAllChips() {
        // All Pets - Brown (default)
        chipAll.setBackgroundColor(Color.parseColor("#7A4F2B"))
        chipAll.setTextColor(Color.WHITE)
        chipAll.alpha = 0.6f  // Dim when not selected

        // Lost - Red (default)
        chipLost.setBackgroundColor(Color.parseColor("#F44336"))
        chipLost.setTextColor(Color.WHITE)
        chipLost.alpha = 0.6f  // Dim when not selected

        // Found - Green (default)
        chipFound.setBackgroundColor(Color.parseColor("#4CAF50"))
        chipFound.setTextColor(Color.WHITE)
        chipFound.alpha = 0.6f  // Dim when not selected

        // For Adoption - Blue (default)
        chipAdoption.setBackgroundColor(Color.parseColor("#2196F3"))
        chipAdoption.setTextColor(Color.WHITE)
        chipAdoption.alpha = 0.6f  // Dim when not selected
    }

    private fun performSearch(query: String) {
        Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()
    }

    private fun loadPosts() {
        // Get all posts from database
        allPosts = UserDatabase.getAllPosts(this)
        filterPosts()
    }

    private fun filterPosts() {
        filteredPosts = when (currentFilter) {
            "All" -> allPosts
            else -> allPosts.filter { it.status.equals(currentFilter, ignoreCase = true) }
        }

        updateDisplay()
    }

    private fun updateDisplay() {
        if (filteredPosts.isEmpty()) {
            // Show empty state
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            // Show grid
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE

            // Set adapter
            val adapter = FindAdapter(filteredPosts) { post ->
                showPostPreview(post)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun showPostPreview(post: Post) {
        val message = """
            ${post.petName}
            Status: ${post.status}
            Location: ${post.location}
            Posted by: ${post.userName}
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }
}