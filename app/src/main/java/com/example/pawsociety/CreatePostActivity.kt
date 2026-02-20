package com.example.pawsociety

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreatePostActivity : AppCompatActivity() {

    private var selectedStatus: String = "Lost"
    private lateinit var etPetName: EditText
    private lateinit var actPetType: AutoCompleteTextView
    private lateinit var etReward: EditText
    private lateinit var tvLocation: TextView
    private lateinit var btnSelectLocation: Button
    private lateinit var etContact: EditText
    private lateinit var etDescription: EditText

    private lateinit var btnStatusLost: TextView
    private lateinit var btnStatusFound: TextView
    private lateinit var btnStatusAdoption: TextView

    private lateinit var layoutReward: LinearLayout

    private lateinit var btnPost: TextView
    private lateinit var btnCancel: TextView
    private lateinit var progressBar: ProgressBar

    // Error TextViews
    private lateinit var errorPetName: TextView
    private lateinit var errorPetType: TextView
    private lateinit var errorStatus: TextView
    private lateinit var errorLocation: TextView
    private lateinit var errorContact: TextView
    private lateinit var errorDescription: TextView

    private val postRepository = PostRepository()
    private val selectedImageUris = mutableListOf<Uri>()

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val validation = FileValidator.validateImage(this, it)
            if (validation is Resource.Success) {
                selectedImageUris.add(it)
                updatePhotoPreview()
            } else if (validation is Resource.Error) {
                Toast.makeText(this, validation.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        lifecycleScope.launch {
            val currentUser = UserRepository().getCurrentUser()
            if (currentUser == null) {
                Toast.makeText(this@CreatePostActivity, "Please login to create posts", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            setupClickListeners(currentUser)
        }

        initializeViews()
        setupValidationListeners()
    }

    private fun initializeViews() {
        etPetName = findViewById(R.id.et_pet_name)
        actPetType = findViewById(R.id.act_pet_type)
        etReward = findViewById(R.id.et_reward)
        tvLocation = findViewById(R.id.tv_location)
        btnSelectLocation = findViewById(R.id.btn_select_location)
        etContact = findViewById(R.id.et_contact)
        etDescription = findViewById(R.id.et_description)

        btnStatusLost = findViewById(R.id.btn_status_lost)
        btnStatusFound = findViewById(R.id.btn_status_found)
        btnStatusAdoption = findViewById(R.id.btn_status_adoption)

        layoutReward = findViewById(R.id.layout_reward)

        btnPost = findViewById(R.id.btn_post)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)

        // Create error TextViews
        errorPetName = createErrorTextView()
        errorPetType = createErrorTextView()
        errorStatus = createErrorTextView()
        errorLocation = createErrorTextView()
        errorContact = createErrorTextView()
        errorDescription = createErrorTextView()

        // Add error views after each input
        addErrorViewAfter(etPetName, errorPetName)
        addErrorViewAfter(actPetType, errorPetType)
        addErrorViewAfter(btnSelectLocation, errorLocation)
        addErrorViewAfter(etContact, errorContact)
        addErrorViewAfter(etDescription, errorDescription)

        // Status error goes after status buttons
        val statusLayout = findViewById<LinearLayout>(R.id.status_buttons_layout)
        addErrorViewAfter(statusLayout, errorStatus)

        // Set initial status
        updateStatusButtons(btnStatusLost)

        // Initialize location TextView
        tvLocation.text = ""
        tvLocation.visibility = View.GONE
    }

    private fun createErrorTextView(): TextView {
        val textView = TextView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = 4
        layoutParams.bottomMargin = 8
        textView.layoutParams = layoutParams
        textView.textSize = 12f
        textView.setTextColor(Color.parseColor("#F44336"))
        textView.visibility = View.GONE
        return textView
    }

    private fun addErrorViewAfter(view: View, errorView: TextView) {
        val parent = view.parent as? ViewGroup
        if (parent != null) {
            val index = parent.indexOfChild(view)
            parent.addView(errorView, index + 1)
        }
    }

    private fun setupValidationListeners() {
        // Pet Name validation
        etPetName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validatePetName() }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Contact validation
        etContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validateContact() }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Description validation
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validateDescription() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners(currentUser: AppUser) {
        btnCancel.setOnClickListener { finish() }

        findViewById<TextView>(R.id.btn_add_photo).setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnSelectLocation.setOnClickListener {
            // Simplified location picker for this example
            tvLocation.text = "Manila, Philippines"
            tvLocation.visibility = View.VISIBLE
            validateLocation()
        }

        btnStatusLost.setOnClickListener {
            updateStatusButtons(btnStatusLost)
            selectedStatus = "Lost"
            layoutReward.visibility = View.VISIBLE
        }

        btnStatusFound.setOnClickListener {
            updateStatusButtons(btnStatusFound)
            selectedStatus = "Found"
            layoutReward.visibility = View.GONE
        }

        btnStatusAdoption.setOnClickListener {
            updateStatusButtons(btnStatusAdoption)
            selectedStatus = "Adoption"
            layoutReward.visibility = View.GONE
        }

        btnPost.setOnClickListener {
            if (validateAllFields()) {
                createNewPost(currentUser)
            }
        }
    }

    private fun updateStatusButtons(selected: TextView) {
        btnStatusLost.alpha = 0.5f
        btnStatusFound.alpha = 0.5f
        btnStatusAdoption.alpha = 0.5f
        selected.alpha = 1.0f
    }

    private fun updatePhotoPreview() {
        // Show number of selected photos
        findViewById<TextView>(R.id.btn_add_photo).text = "Photos Added: ${selectedImageUris.size}"
    }

    private fun validatePetName(): Boolean {
        val name = etPetName.text.toString().trim()
        return if (name.isEmpty()) {
            errorPetName.text = "Pet name is required"
            errorPetName.visibility = View.VISIBLE
            false
        } else {
            errorPetName.visibility = View.GONE
            true
        }
    }

    private fun validateContact(): Boolean {
        val contact = etContact.text.toString().trim()
        return if (contact.isEmpty()) {
            errorContact.text = "Contact is required"
            errorContact.visibility = View.VISIBLE
            false
        } else {
            errorContact.visibility = View.GONE
            true
        }
    }

    private fun validateDescription(): Boolean {
        val desc = etDescription.text.toString().trim()
        return if (desc.isEmpty()) {
            errorDescription.text = "Description is required"
            errorDescription.visibility = View.VISIBLE
            false
        } else {
            errorDescription.visibility = View.GONE
            true
        }
    }

    private fun validateLocation(): Boolean {
        return if (tvLocation.text.isEmpty()) {
            errorLocation.text = "Location is required"
            errorLocation.visibility = View.VISIBLE
            false
        } else {
            errorLocation.visibility = View.GONE
            true
        }
    }

    private fun validateAllFields(): Boolean {
        return validatePetName() && validateContact() && validateDescription() && validateLocation()
    }

    private fun createNewPost(currentUser: AppUser) {
        Log.d("CreatePostActivity", "========== START CREATE POST ==========")
        Log.d("CreatePostActivity", "User: ${currentUser.uid} (${currentUser.username})")
        Log.d("CreatePostActivity", "Emulator connected: ${MyApplication.isConnectedToEmulator}")
        
        val post = Post(
            userName = currentUser.username,
            userImageUrl = currentUser.profileImageUrl,
            petName = etPetName.text.toString().trim(),
            petType = actPetType.text.toString().trim(),
            status = selectedStatus,
            description = etDescription.text.toString().trim(),
            location = tvLocation.text.toString(),
            reward = etReward.text.toString().trim(),
            contactInfo = etContact.text.toString().trim(),
            createdAt = com.google.firebase.Timestamp.now()
        )

        Log.d("CreatePostActivity", "Post data: $post")
        Log.d("CreatePostActivity", "Images: ${selectedImageUris.size}")

        lifecycleScope.launch {
            try {
                if (::progressBar.isInitialized) {
                    progressBar.visibility = View.VISIBLE
                }
                if (::btnPost.isInitialized) {
                    btnPost.isEnabled = false
                }

                Log.d("CreatePostActivity", "Calling repository...")
                val result = postRepository.createPost(this@CreatePostActivity, post, selectedImageUris)

                if (::progressBar.isInitialized) {
                    progressBar.visibility = View.GONE
                }
                if (::btnPost.isInitialized) {
                    btnPost.isEnabled = true
                }

                if (result is Resource.Success) {
                    Log.d("CreatePostActivity", "✓ Post created successfully: ${result.data}")
                    Toast.makeText(this@CreatePostActivity, "Post Created!", Toast.LENGTH_SHORT).show()
                    finish()
                } else if (result is Resource.Error) {
                    Log.e("CreatePostActivity", "✗ Post creation failed: ${result.message}")
                    Toast.makeText(this@CreatePostActivity, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("CreatePostActivity", "========== EXCEPTION ==========")
                Log.e("CreatePostActivity", "Error: ${e.message}", e)
                e.printStackTrace()
                
                if (::progressBar.isInitialized) {
                    progressBar.visibility = View.GONE
                }
                if (::btnPost.isInitialized) {
                    btnPost.isEnabled = true
                }
                Toast.makeText(this@CreatePostActivity, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
