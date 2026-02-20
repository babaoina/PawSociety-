package com.example.pawsociety

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // Error TextViews
    private lateinit var errorPetName: TextView
    private lateinit var errorPetType: TextView
    private lateinit var errorStatus: TextView
    private lateinit var errorLocation: TextView
    private lateinit var errorContact: TextView
    private lateinit var errorDescription: TextView

    // Adapter for breed suggestions
    private lateinit var breedAdapter: SuggestionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        // Check if user is logged in
        val currentUser = UserDatabase.getCurrentUser(this)
        if (currentUser == null) {
            Toast.makeText(this, "Please login to create posts", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupAdapters()
        setupValidationListeners()
        setupClickListeners(currentUser)
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

    private fun setupAdapters() {
        // Breed suggestions adapter
        breedAdapter = SuggestionsAdapter()
        breedAdapter.setData(PetData.getAllBreeds())
        actPetType.setAdapter(breedAdapter)
        actPetType.threshold = 1

        // Handle item clicks for breed
        actPetType.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selected = breedAdapter.getItem(position) as String
            actPetType.setText(selected)
            validatePetType()
        }
    }

    private fun setupValidationListeners() {
        // Pet Name validation
        etPetName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePetName()
            }

            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty() && s.length == 1) {
                    etPetName.removeTextChangedListener(this)
                    etPetName.setText(s.toString().uppercase())
                    etPetName.setSelection(1)
                    etPetName.addTextChangedListener(this)
                }
            }
        })

        // Fix enter key
        etPetName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                actPetType.requestFocus()
                true
            } else {
                false
            }
        }

        // Pet Type validation
        actPetType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePetType()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        actPetType.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                btnSelectLocation.requestFocus()
                true
            } else {
                false
            }
        }

        // REWARD FIELD - With formatting and 1 million limit
        etReward.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val input = s.toString()
                if (input.isEmpty()) return

                // Remove existing commas for processing
                val rawInput = input.replace(",", "")

                try {
                    val number = rawInput.toLongOrNull()
                    if (number != null) {
                        isUpdating = true

                        // Limit to 1 million
                        val limitedNumber = if (number > 1000000) {
                            Toast.makeText(this@CreatePostActivity, "Maximum reward is ₱1,000,000", Toast.LENGTH_SHORT).show()
                            1000000
                        } else {
                            number
                        }

                        // Format with commas
                        val formatted = String.format("%,d", limitedNumber)

                        if (formatted != input) {
                            etReward.setText(formatted)
                            etReward.setSelection(formatted.length)
                        }

                        isUpdating = false
                    }
                } catch (e: Exception) {
                    isUpdating = false
                }
            }
        })

        etReward.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                btnSelectLocation.requestFocus()
                true
            } else {
                false
            }
        }

        // Contact validation
        etContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateContact()
            }
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val digits = input.filter { it.isDigit() }
                if (digits != input && digits.length <= 11) {
                    etContact.removeTextChangedListener(this)
                    etContact.setText(digits)
                    etContact.setSelection(digits.length)
                    etContact.addTextChangedListener(this)
                }
            }
        })

        etContact.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                etDescription.requestFocus()
                true
            } else {
                false
            }
        }

        // Description validation
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateDescription()
            }
            override fun afterTextChanged(s: Editable?) {
                updateCharCounter()
            }
        })

        etDescription.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun updateCharCounter() {
        val charCount = etDescription.text.toString().length
        val counterView = findViewById<TextView>(R.id.tv_char_counter)
        counterView.text = "$charCount/500 characters"

        when {
            charCount > 450 -> counterView.setTextColor(Color.parseColor("#FF9800"))
            charCount >= 500 -> counterView.setTextColor(Color.parseColor("#F44336"))
            else -> counterView.setTextColor(Color.parseColor("#999999"))
        }
    }

    private fun setupClickListeners(currentUser: AppUser) {
        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Add photo button
        findViewById<TextView>(R.id.btn_add_photo).setOnClickListener {
            Toast.makeText(this, "Open camera or gallery", Toast.LENGTH_SHORT).show()
        }

        // Location selector button
        btnSelectLocation.setOnClickListener {
            try {
                val dialog = LocationPickerDialog(this) { fullLocation ->
                    tvLocation.text = fullLocation
                    tvLocation.visibility = View.VISIBLE
                    validateLocation()
                }
                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error opening location picker", Toast.LENGTH_SHORT).show()
            }
        }

        // Status buttons
        btnStatusLost.setOnClickListener {
            updateStatusButtons(btnStatusLost)
            selectedStatus = "Lost"
            layoutReward.visibility = View.VISIBLE
            errorStatus.visibility = View.GONE
        }

        btnStatusFound.setOnClickListener {
            updateStatusButtons(btnStatusFound)
            selectedStatus = "Found"
            layoutReward.visibility = View.GONE
            etReward.text.clear()
            errorStatus.visibility = View.GONE
        }

        btnStatusAdoption.setOnClickListener {
            updateStatusButtons(btnStatusAdoption)
            selectedStatus = "Adoption"
            layoutReward.visibility = View.GONE
            etReward.text.clear()
            errorStatus.visibility = View.GONE
        }

        // Post button
        btnPost.setOnClickListener {
            if (validateAllFields()) {
                createNewPost(currentUser)
            }
        }
    }

    private fun updateStatusButtons(selected: TextView) {
        btnStatusLost.setBackgroundColor(Color.parseColor("#F44336"))
        btnStatusLost.setTextColor(Color.WHITE)
        btnStatusFound.setBackgroundColor(Color.parseColor("#4CAF50"))
        btnStatusFound.setTextColor(Color.WHITE)
        btnStatusAdoption.setBackgroundColor(Color.parseColor("#2196F3"))
        btnStatusAdoption.setTextColor(Color.WHITE)

        if (selected != btnStatusLost) btnStatusLost.alpha = 0.5f
        if (selected != btnStatusFound) btnStatusFound.alpha = 0.5f
        if (selected != btnStatusAdoption) btnStatusAdoption.alpha = 0.5f

        selected.alpha = 1.0f
    }

    private fun validatePetName(): Boolean {
        val name = etPetName.text.toString().trim()

        return when {
            name.isEmpty() -> {
                etPetName.setBackgroundResource(R.drawable.edittext_error_bg)
                errorPetName.text = "Pet name is required"
                errorPetName.visibility = View.VISIBLE
                false
            }
            name.length > 10 -> {
                etPetName.setBackgroundResource(R.drawable.edittext_error_bg)
                errorPetName.text = "Pet name must be max 10 characters"
                errorPetName.visibility = View.VISIBLE
                false
            }
            else -> {
                etPetName.setBackgroundResource(R.drawable.edittext_bg)
                errorPetName.visibility = View.GONE
                true
            }
        }
    }

    private fun validatePetType(): Boolean {
        val type = actPetType.text.toString().trim()

        return when {
            type.isEmpty() -> {
                actPetType.setBackgroundResource(R.drawable.edittext_error_bg)
                errorPetType.text = "Pet type/breed is required"
                errorPetType.visibility = View.VISIBLE
                false
            }
            else -> {
                actPetType.setBackgroundResource(R.drawable.edittext_bg)
                errorPetType.visibility = View.GONE
                true
            }
        }
    }

    private fun validateStatus(): Boolean {
        return if (selectedStatus.isEmpty()) {
            errorStatus.text = "Please select a status"
            errorStatus.visibility = View.VISIBLE
            false
        } else {
            errorStatus.visibility = View.GONE
            true
        }
    }

    private fun validateLocation(): Boolean {
        val location = tvLocation.text.toString()

        return when {
            location.isEmpty() -> {
                btnSelectLocation.setBackgroundResource(R.drawable.edittext_error_bg)
                errorLocation.text = "Location is required"
                errorLocation.visibility = View.VISIBLE
                false
            }
            else -> {
                btnSelectLocation.setBackgroundResource(R.drawable.edittext_bg)
                errorLocation.visibility = View.GONE
                true
            }
        }
    }

    private fun validateContact(): Boolean {
        val contact = etContact.text.toString().trim()

        return when {
            contact.isEmpty() -> {
                etContact.setBackgroundResource(R.drawable.edittext_error_bg)
                errorContact.text = "Contact number is required"
                errorContact.visibility = View.VISIBLE
                false
            }
            !contact.matches(Regex("^09\\d{9}$")) -> {
                etContact.setBackgroundResource(R.drawable.edittext_error_bg)
                errorContact.text = "Must be 11 digits starting with 09"
                errorContact.visibility = View.VISIBLE
                false
            }
            else -> {
                etContact.setBackgroundResource(R.drawable.edittext_bg)
                errorContact.visibility = View.GONE
                true
            }
        }
    }

    private fun validateDescription(): Boolean {
        val description = etDescription.text.toString().trim()

        return when {
            description.isEmpty() -> {
                etDescription.setBackgroundResource(R.drawable.edittext_error_bg)
                errorDescription.text = "Description is required"
                errorDescription.visibility = View.VISIBLE
                false
            }
            description.length < 10 -> {
                etDescription.setBackgroundResource(R.drawable.edittext_error_bg)
                errorDescription.text = "Description must be at least 10 characters"
                errorDescription.visibility = View.VISIBLE
                false
            }
            description.length > 500 -> {
                etDescription.setBackgroundResource(R.drawable.edittext_error_bg)
                errorDescription.text = "Description must not exceed 500 characters"
                errorDescription.visibility = View.VISIBLE
                false
            }
            else -> {
                etDescription.setBackgroundResource(R.drawable.edittext_bg)
                errorDescription.visibility = View.GONE
                true
            }
        }
    }

    private fun validateAllFields(): Boolean {
        val isPetNameValid = validatePetName()
        val isPetTypeValid = validatePetType()
        val isStatusValid = validateStatus()
        val isLocationValid = validateLocation()
        val isContactValid = validateContact()
        val isDescriptionValid = validateDescription()

        return isPetNameValid && isPetTypeValid && isStatusValid &&
                isLocationValid && isContactValid && isDescriptionValid
    }

    private fun createNewPost(currentUser: AppUser) {
        val petName = etPetName.text.toString().trim()
        val petType = actPetType.text.toString().trim()

        // Remove commas from reward before saving
        val rewardRaw = etReward.text.toString().trim().replace(",", "")
        val reward = if (selectedStatus == "Lost") rewardRaw else ""

        val location = tvLocation.text.toString()
        val contact = etContact.text.toString().trim()
        val description = etDescription.text.toString().trim()

        val postId = "post_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"

        val post = Post(
            postId = postId,
            userId = currentUser.uid,
            userName = currentUser.username,
            userImageUrl = currentUser.profileImageUrl,
            petName = petName,
            petType = petType,
            status = selectedStatus,
            description = description,
            location = location,
            reward = reward,
            contactInfo = contact,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        // Show loading state
        btnPost.text = "Posting..."
        btnPost.isEnabled = false

        val success = UserDatabase.savePost(this, post)

        if (success) {
            Toast.makeText(this, "✅ Post created successfully!", Toast.LENGTH_SHORT).show()
            clearForm()
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "❌ Failed to create post", Toast.LENGTH_SHORT).show()
            btnPost.text = "Post"
            btnPost.isEnabled = true
        }
    }

    private fun clearForm() {
        etPetName.text.clear()
        actPetType.text.clear()
        etReward.text.clear()
        tvLocation.text = ""
        tvLocation.visibility = View.GONE
        etContact.text.clear()
        etDescription.text.clear()

        // Reset to Lost as default
        updateStatusButtons(btnStatusLost)
        selectedStatus = "Lost"
        layoutReward.visibility = View.VISIBLE

        // Clear all errors
        etPetName.setBackgroundResource(R.drawable.edittext_bg)
        actPetType.setBackgroundResource(R.drawable.edittext_bg)
        btnSelectLocation.setBackgroundResource(R.drawable.edittext_bg)
        etContact.setBackgroundResource(R.drawable.edittext_bg)
        etDescription.setBackgroundResource(R.drawable.edittext_bg)

        errorPetName.visibility = View.GONE
        errorPetType.visibility = View.GONE
        errorStatus.visibility = View.GONE
        errorLocation.visibility = View.GONE
        errorContact.visibility = View.GONE
        errorDescription.visibility = View.GONE

        updateCharCounter()
    }
}