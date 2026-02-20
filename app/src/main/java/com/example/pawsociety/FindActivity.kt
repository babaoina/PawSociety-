package com.example.pawsociety

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FindActivity : BaseNavigationActivity() {

    private val userRepository = UserRepository()
    private val petRepository = PetRepository()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var btnFilter: LinearLayout
    private lateinit var btnAddPet: TextView

    private var currentFilter = "All"
    private var allPets = listOf<Pet>()
    private var filteredPets = listOf<Pet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find)

        try {
            initViews()
            setupClickListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            // Check if user is logged in
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                Toast.makeText(this@FindActivity, "Please login to find pets", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.find_recycler_view)
        emptyState = findViewById(R.id.empty_state)
        searchInput = findViewById(R.id.search_input)
        btnFilter = findViewById(R.id.btn_filter)
        btnAddPet = findViewById(R.id.btn_add_pet)

        // Setup RecyclerView with 3 columns and spacing
        val gridLayoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager

        // Add item decoration for spacing
        val spacing = 4
        recyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacing, true))
    }

    private fun setupClickListeners() {
        // Add pet click
        btnAddPet.setOnClickListener {
            startActivity(Intent(this@FindActivity, AddPetActivity::class.java))
        }

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
    }

    private fun performSearch(query: String) {
        Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()
    }

    private fun loadPets() {
        lifecycleScope.launch {
            // Get all pets from database
            petRepository.getAllPets().collect { pets ->
                allPets = pets
                filterPets()
            }
        }
    }

    private fun filterPets() {
        filteredPets = allPets
        updateDisplay()
    }

    private fun updateDisplay() {
        if (filteredPets.isEmpty()) {
            // Show empty state
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            // Show grid
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE

            // Set adapter
            val adapter = FindAdapter(filteredPets) { pet ->
                showPetPreview(pet)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun showPetPreview(pet: Pet) {
        val message = """
            ${pet.name}
            Breed: ${pet.breed}
            Age: ${pet.age}
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        loadPets()
    }
}