package com.example.pawsociety

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pawsociety.api.ApiPost
import com.example.pawsociety.api.ApiUser
import com.example.pawsociety.data.repository.UploadRepository
import com.example.pawsociety.util.FileHelper
import com.example.pawsociety.util.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : BaseNavigationActivity() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var sessionManager: SessionManager
    private val uploadRepository = UploadRepository()
    private var currentUser: ApiUser? = null
    private var selectedProfileImageUri: android.net.Uri? = null
    
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedProfileImageUri = it
            // Show preview in dialog if dialog is open
            findViewById<ImageView>(R.id.edit_profile_image)?.let { imageView ->
                imageView.setImageURI(it)
            }
        }
    }
    private var currentTab = "posts" // "posts" or "favorites"
    private val highlights = mutableListOf<Highlight>() // Dynamic highlights list

    // Highlight data class
    data class Highlight(val name: String, val emoji: String, val color: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        viewModel.setSessionManager(sessionManager)

        // Check if user is logged in
        currentUser = sessionManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Observe user data from ViewModel (API)
        viewModel.user.observe(this) { user ->
            if (user != null) {
                println("📱 ProfileActivity: Received user data - username: ${user.username}, image: ${user.profileImageUrl}")
                currentUser = user
                updateProfileWithUserData(user)
                setupClickListeners(user)
            } else {
                println("⚠️ ProfileActivity: User data is null")
            }
        }
        
        // Force load fresh data from API
        viewModel.loadUserData()
        
        loadHighlights() // Load saved highlights

        // Set up the big plus button
        findViewById<View>(R.id.big_plus_button)?.setOnClickListener {
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateProfileWithUserData(user: ApiUser) {
        println("🔄 ProfileActivity: Updating UI with user data")
        println("   - Username: ${user.username}")
        println("   - Full Name: ${user.fullName}")
        println("   - Bio: ${user.bio}")
        println("   - Profile Image: ${user.profileImageUrl}")
        
        try {
            findViewById<TextView>(R.id.tv_username)?.text = user.username
            println("✅ Username set to: ${user.username}")

            // Set circular profile picture
            setProfilePicture(user)

            val bioTextView = findViewById<TextView>(R.id.tv_bio)

            // Use REAL bio from MongoDB - NO hardcoded fallback
            val location = if (!user.location.isNullOrEmpty()) user.location else ""
            val bio = if (!user.bio.isNullOrEmpty()) user.bio else ""

            // Build bio
            val newBio = if (location.isNotEmpty() && bio.isNotEmpty()) {
                """
                ${user.fullName}
                $location
                $bio
            """.trimIndent()
            } else if (bio.isNotEmpty()) {
                bio
            } else if (location.isNotEmpty()) {
                """
                ${user.fullName}
                $location
            """.trimIndent()
            } else {
                user.fullName
            }

            bioTextView?.text = newBio
            println("✅ Bio set to: $newBio")

            // Post count will be updated when data loads from ViewModel
            findViewById<TextView>(R.id.tv_post_count)?.text = "0"
            findViewById<TextView>(R.id.tv_follower_count)?.text = "0"
            findViewById<TextView>(R.id.tv_following_count)?.text = "0"

            // Handle posts grid based on current tab
            if (currentTab == "posts") {
                loadPostsTab(user)
            } else {
                loadFavoritesTab(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setProfilePicture(user: ApiUser) {
        try {
            val profileInitial = findViewById<TextView>(R.id.profile_initial)
            val profileImage = findViewById<ImageView>(R.id.profile_image)
            val profileBackground = findViewById<View>(R.id.profile_circle_background)

            // Load profile image if available
            if (!user.profileImageUrl.isNullOrEmpty()) {
                val fullImageUrl = if (user.profileImageUrl.startsWith("http")) {
                    user.profileImageUrl
                } else {
                    "${com.example.pawsociety.api.ApiClient.FULL_BASE_URL}${user.profileImageUrl}"
                }
                
                profileImage?.visibility = View.VISIBLE
                profileInitial?.visibility = View.GONE
                
                Glide.with(this)
                    .load(fullImageUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(profileImage)
            } else {
                // Show initial letter as fallback
                profileImage?.visibility = View.GONE
                profileInitial?.visibility = View.VISIBLE
                
                val color = generateColorFromUsername(user.username)
                val backgroundDrawable = ContextCompat.getDrawable(this, R.drawable.circle_solid_profile)
                backgroundDrawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                profileBackground.background = backgroundDrawable

                // Get first letter of first name
                val firstName = if (user.fullName.contains(",")) {
                    val parts = user.fullName.split(", ")
                    if (parts.size > 1) {
                        parts[1].split(" ").firstOrNull() ?: ""
                    } else {
                        user.fullName
                    }
                } else {
                    user.fullName.split(" ").firstOrNull() ?: ""
                }

                val firstLetter = if (firstName.isNotEmpty()) {
                    firstName.first().toString().uppercase()
                } else {
                    "?"
                }

                profileInitial.text = firstLetter
                profileInitial.textSize = 36f
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                findViewById<TextView>(R.id.profile_initial)?.text = "?"
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    private fun generateColorFromUsername(username: String): Int {
        // Generate a consistent color based on username hash
        val colors = listOf(
            "#FF6B35", // Orange
            "#4CAF50", // Green
            "#2196F3", // Blue
            "#9C27B0", // Purple
            "#F44336", // Red
            "#009688", // Teal
            "#FF9800", // Orange darker
            "#3F51B5", // Indigo
            "#E91E63", // Pink
            "#7A4F2B"  // Brown
        )

        val hash = Math.abs(username.hashCode())
        val index = hash % colors.size
        return Color.parseColor(colors[index])
    }

    private fun loadHighlights() {
        // Load highlights from database
        currentUser?.let { user ->
            val savedHighlights = UserDatabase.getUserHighlights(this, user.firebaseUid)
            highlights.clear()
            highlights.addAll(savedHighlights)
            updateHighlightsDisplay()
        }
    }

    private fun saveHighlights() {
        currentUser?.let { user ->
            UserDatabase.saveUserHighlights(this, user.firebaseUid, highlights)
        }
    }

    private fun updateHighlightsDisplay() {
        val highlightsContainer = findViewById<LinearLayout>(R.id.highlights_container)
        highlightsContainer.removeAllViews()

        // Add "New" button first
        addNewHighlightButton(highlightsContainer)

        // Add all user-created highlights
        for (highlight in highlights) {
            addHighlightView(highlightsContainer, highlight)
        }
    }

    private fun addNewHighlightButton(container: LinearLayout) {
        val newHighlightView = layoutInflater.inflate(R.layout.item_highlight_new, container, false)

        newHighlightView.setOnClickListener { view ->
            // Button press animation
            view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(150).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            showCreateHighlightDialog()
        }

        container.addView(newHighlightView)
    }

    private fun addHighlightView(container: LinearLayout, highlight: Highlight) {
        val highlightView = layoutInflater.inflate(R.layout.item_highlight, container, false)

        val circleBackground = highlightView.findViewById<View>(R.id.highlight_circle_background)
        val circleText = highlightView.findViewById<TextView>(R.id.highlight_circle_text)
        val titleText = highlightView.findViewById<TextView>(R.id.highlight_title)

        // Use ColorFilter to change the color while preserving the shape
        val backgroundDrawable = ContextCompat.getDrawable(this, R.drawable.circle_solid_highlight)
        backgroundDrawable?.setColorFilter(Color.parseColor(highlight.color), PorterDuff.Mode.SRC_ATOP)
        circleBackground.background = backgroundDrawable

        // Set emoji and title
        circleText.text = highlight.emoji
        circleText.setTextColor(Color.WHITE)
        titleText.text = highlight.name

        highlightView.setOnClickListener { view ->
            // Button press animation
            view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(150).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            Toast.makeText(this, "Viewing ${highlight.name} highlight", Toast.LENGTH_SHORT).show()
        }

        container.addView(highlightView)
    }

    private fun showCreateHighlightDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_highlight, null)

        val etName = dialogView.findViewById<EditText>(R.id.et_highlight_name)
        val emojiPicker = dialogView.findViewById<Spinner>(R.id.spinner_emoji)
        val colorPicker = dialogView.findViewById<Spinner>(R.id.spinner_color)

        // Emoji options
        val emojis = arrayOf("🐶", "🐱", "🐰", "🐦", "🐟", "🐾", "❤️", "⭐", "🏆", "📸")
        val emojiAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, emojis)
        emojiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        emojiPicker.adapter = emojiAdapter

        // Color options with hex values
        val colorOptions = arrayOf(
            "Orange" to "#FF6B35",
            "Green" to "#4CAF50",
            "Blue" to "#2196F3",
            "Purple" to "#9C27B0",
            "Red" to "#F44336",
            "Teal" to "#009688"
        )
        val colorNames = colorOptions.map { it.first }.toTypedArray()
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorNames)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorPicker.adapter = colorAdapter

        var selectedEmoji = "🐶"
        var selectedColor = "#FF6B35" // Default orange

        emojiPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEmoji = emojis[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        colorPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedColor = colorOptions[position].second // Get the hex value
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Create New Highlight")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newHighlight = Highlight(name, selectedEmoji, selectedColor)
                    highlights.add(newHighlight)
                    saveHighlights()
                    updateHighlightsDisplay()

                    // Scroll to the end
                    val scrollView = findViewById<HorizontalScrollView>(R.id.highlights_scroll)
                    scrollView.post {
                        scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
                    }

                    Toast.makeText(this, "Highlight created!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseFullName(fullName: String): Triple<String, String, String> {
        return try {
            val parts = fullName.split(", ")
            val lastName = parts.getOrElse(0) { "" }
            val firstMiddle = parts.getOrElse(1) { "" }.split(" ")
            val firstName = firstMiddle.getOrElse(0) { "" }
            val middleInitial = if (firstMiddle.size > 1)
                firstMiddle[1].replace(".", "") else ""
            Triple(lastName, firstName, middleInitial)
        } catch (e: Exception) {
            // If not in "Last, First" format, try to parse normally
            val nameParts = fullName.split(" ")
            when (nameParts.size) {
                1 -> Triple(nameParts[0], "", "")
                2 -> Triple(nameParts[0], nameParts[1], "")
                3 -> Triple(nameParts[0], nameParts[1], nameParts[2])
                else -> Triple("", "", "")
            }
        }
    }

    private fun showProfilePhotoPicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private suspend fun uploadProfileImage(): String? {
        val uri = selectedProfileImageUri ?: return null

        val file = FileHelper.uriToFile(this, uri) ?: return null
        val result = uploadRepository.uploadProfilePicture(file)

        return result.getOrNull()
    }

    private fun createPostsGrid(container: LinearLayout, posts: List<ApiPost>) {
        try {
            val colors = listOf(
                "#4CAF50", "#2196F3", "#FF9800",
                "#9C27B0", "#FF5722", "#00BCD4",
                "#E91E63", "#3F51B5", "#009688"
            )

            val rows = (posts.size + 2) / 3
            val spacing = 4 // 4 pixels spacing between items

            for (row in 0 until rows) {
                val rowLayout = LinearLayout(this)
                rowLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLayout.orientation = LinearLayout.HORIZONTAL

                for (col in 0 until 3) {
                    val postIndex = row * 3 + col

                    val squareContainer = FrameLayout(this)
                    val squareParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                    // Add margins to create spacing
                    squareParams.setMargins(
                        if (col == 0) 0 else spacing,  // left margin for columns 2 and 3
                        0,
                        0,
                        if (row < rows - 1) spacing else 0  // bottom margin for all except last row
                    )

                    squareContainer.layoutParams = squareParams

                    squareContainer.post {
                        val width = squareContainer.width
                        squareContainer.layoutParams.height = width
                        squareContainer.requestLayout()
                    }

                    if (postIndex < posts.size) {
                        val post = posts[postIndex]
                        val postView = layoutInflater.inflate(R.layout.item_profile_post, squareContainer, false)
                        val postContent = postView.findViewById<TextView>(R.id.post_content)
                        val postImage = postView.findViewById<ImageView>(R.id.post_image)

                        // Load image if available
                        if (!post.imageUrls.isNullOrEmpty() && post.imageUrls.isNotEmpty()) {
                            val imageUrl = post.imageUrls[0]
                            val fullImageUrl = if (imageUrl.startsWith("http")) {
                                imageUrl
                            } else {
                                "${com.example.pawsociety.api.ApiClient.FULL_BASE_URL}$imageUrl"
                            }
                            
                            postImage?.visibility = View.VISIBLE
                            Glide.with(this)
                                .load(fullImageUrl)
                                .centerCrop()
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(postImage)
                        } else {
                            // Show colored background with pet info
                            postImage?.visibility = View.GONE
                            postContent.text = "${getPetEmoji(post.petType)}\n${post.petName}"
                            postContent.setBackgroundColor(Color.parseColor(colors[postIndex % colors.size]))
                        }

                        postView.setOnClickListener {
                            showPostDetails(post)
                        }

                        squareContainer.addView(postView)
                    } else {
                        val emptyView = View(this)
                        emptyView.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        emptyView.setBackgroundColor(Color.parseColor("#F0F0F0"))
                        squareContainer.addView(emptyView)
                    }

                    rowLayout.addView(squareContainer)
                }

                container.addView(rowLayout)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPetEmoji(petType: String): String {
        return when {
            petType.contains("dog", ignoreCase = true) -> "🐶"
            petType.contains("cat", ignoreCase = true) -> "🐱"
            petType.contains("bird", ignoreCase = true) -> "🐦"
            petType.contains("rabbit", ignoreCase = true) -> "🐰"
            petType.contains("fish", ignoreCase = true) -> "🐟"
            else -> "🐾"
        }
    }

    private fun showPostDetails(post: ApiPost) {
        val details = """
            🐾 ${post.petName}
            👤 Posted by: ${post.userName}
            📍 ${post.location}
            🔍 Status: ${post.status}
            💰 ${if (!post.reward.isNullOrEmpty()) "Reward: ${post.reward}" else "No reward"}
            📞 ${post.contactInfo}

            ${post.description}

            🕒 ${post.createdAt}
        """.trimIndent()

        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
    }

    private fun setupClickListeners(user: ApiUser) {
        try {
            findViewById<TextView>(R.id.btn_settings)?.setOnClickListener {
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            findViewById<TextView>(R.id.btn_edit_profile)?.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

                showEditProfileDialog(user)
            }

            // Posts Tab Click - Icons only
            findViewById<View>(R.id.tab_posts)?.setOnClickListener {
                if (currentTab != "posts") {
                    currentTab = "posts"

                    // Update tab colors - only icons
                    findViewById<TextView>(R.id.tab_posts_icon)?.setTextColor(Color.parseColor("#B88B4A"))
                    findViewById<ImageView>(R.id.tab_favorites_icon)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_ATOP)

                    loadPostsTab(user)
                }
            }

            // Favorites Tab Click - Icons only
            findViewById<View>(R.id.tab_favorites)?.setOnClickListener {
                if (currentTab != "favorites") {
                    currentTab = "favorites"

                    // Update tab colors - only icons
                    findViewById<ImageView>(R.id.tab_favorites_icon)?.setColorFilter(Color.parseColor("#B88B4A"), PorterDuff.Mode.SRC_ATOP)
                    findViewById<TextView>(R.id.tab_posts_icon)?.setTextColor(Color.parseColor("#FFFFFF"))

                    loadFavoritesTab(user)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPostsTab(user: ApiUser) {
        try {
            // Show user's posts from ViewModel (API)
            val postsGrid = findViewById<LinearLayout>(R.id.posts_grid)
            val bigPlusButton = findViewById<View>(R.id.big_plus_button)

            if (postsGrid == null) {
                Toast.makeText(this, "Error: posts_grid not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Observe user posts from ViewModel
            viewModel.userPosts.observe(this) { userPosts ->
                postsGrid.removeAllViews()

                if (userPosts.isNotEmpty()) {
                    bigPlusButton?.visibility = View.GONE
                    postsGrid.visibility = View.VISIBLE
                    createPostsGrid(postsGrid, userPosts)
                    
                    // Update post count
                    findViewById<TextView>(R.id.tv_post_count)?.text = userPosts.size.toString()
                } else {
                    bigPlusButton?.visibility = View.VISIBLE
                    postsGrid.visibility = View.GONE
                    findViewById<TextView>(R.id.tv_post_count)?.text = "0"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading posts: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFavoritesTab(user: ApiUser) {
        try {
            // Get user's favorite posts from ViewModel (API)
            val postsGrid = findViewById<LinearLayout>(R.id.posts_grid)
            val bigPlusButton = findViewById<View>(R.id.big_plus_button)

            if (postsGrid == null) {
                Toast.makeText(this, "Error: posts_grid not found", Toast.LENGTH_SHORT).show()
                return
            }

            postsGrid.removeAllViews()

            // Hide big plus button
            bigPlusButton?.visibility = View.GONE
            postsGrid.visibility = View.VISIBLE

            // Observe favorite posts from ViewModel
            viewModel.favoritePosts.observe(this) { favoritePosts ->
                if (favoritePosts.isNotEmpty()) {
                    // Show favorite posts in grid
                    createPostsGrid(postsGrid, favoritePosts)
                } else {
                    // Show empty state
                    val emptyView = TextView(this)
                    emptyView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    emptyView.gravity = android.view.Gravity.CENTER
                    emptyView.text = "No favorites yet\nSave posts you like from the home feed!"
                    emptyView.setTextColor(Color.parseColor("#999999"))
                    emptyView.textSize = 16f
                    emptyView.setPadding(0, 100, 0, 100)
                    postsGrid.addView(emptyView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading favorites: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditProfileDialog(user: ApiUser) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)

        val etLastName = dialogView.findViewById<EditText>(R.id.et_last_name)
        val etFirstName = dialogView.findViewById<EditText>(R.id.et_first_name)
        val etMiddleInitial = dialogView.findViewById<EditText>(R.id.et_middle_initial)
        val etUsername = dialogView.findViewById<EditText>(R.id.et_username)
        val etBio = dialogView.findViewById<EditText>(R.id.et_bio)
        val profileImage = dialogView.findViewById<ImageView>(R.id.edit_profile_image)
        val btnChangePhoto = dialogView.findViewById<TextView>(R.id.btn_change_photo)

        val (lastName, firstName, middleInitial) = parseFullName(user.fullName)
        etLastName.setText(lastName)
        etFirstName.setText(firstName)
        etMiddleInitial.setText(middleInitial)
        etUsername.setText(user.username)
        etBio.setText(user.bio)
        
        // Load current profile image
        if (!user.profileImageUrl.isNullOrEmpty()) {
            val fullImageUrl = if (user.profileImageUrl.startsWith("http")) {
                user.profileImageUrl
            } else {
                "${com.example.pawsociety.api.ApiClient.FULL_BASE_URL}${user.profileImageUrl}"
            }
            Glide.with(this)
                .load(fullImageUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(profileImage)
        }
        
        // Change photo button
        btnChangePhoto.setOnClickListener {
            showProfilePhotoPicker()
        }

        setupValidation(etLastName, etFirstName, etUsername)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                lifecycleScope.launch {
                    if (validateFields(etLastName, etFirstName, etUsername)) {
                        val newLastName = etLastName.text.toString().trim()
                        val newFirstName = etFirstName.text.toString().trim()
                        val newMiddleInitial = etMiddleInitial.text.toString().trim()

                        val newFullName = if (newMiddleInitial.isNotEmpty()) {
                            "$newLastName, $newFirstName $newMiddleInitial."
                        } else {
                            "$newLastName, $newFirstName"
                        }

                        val newUsername = etUsername.text.toString().trim()
                        val newBio = etBio.text.toString().trim()

                        if (newUsername != user.username) {
                            if (UserDatabase.isUsernameTaken(this@ProfileActivity, newUsername, user.firebaseUid)) {
                                Toast.makeText(this@ProfileActivity, "Username already taken", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                        }

                        // Upload profile image if selected
                        var profileImageUrl = user.profileImageUrl
                        if (selectedProfileImageUri != null) {
                            val uploadedUrl = uploadProfileImage()
                            if (uploadedUrl != null) {
                                profileImageUrl = uploadedUrl
                            }
                        }

                        // Update profile using ViewModel (API)
                        viewModel.updateProfile(
                            fullName = newFullName,
                            username = newUsername,
                            bio = newBio,
                            profileImageUrl = profileImageUrl
                        )

                        Toast.makeText(this@ProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        
                        // Force refresh UI after dialog closes
                        viewModel.user.value?.let { user ->
                            updateProfileWithUserData(user)
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun setupValidation(
        etLastName: EditText,
        etFirstName: EditText,
        etUsername: EditText
    ) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateFields(etLastName, etFirstName, etUsername)
            }
        }

        etLastName.addTextChangedListener(textWatcher)
        etFirstName.addTextChangedListener(textWatcher)
        etUsername.addTextChangedListener(textWatcher)
    }

    private fun validateFields(
        etLastName: EditText,
        etFirstName: EditText,
        etUsername: EditText
    ): Boolean {
        var isValid = true

        val lastName = etLastName.text.toString().trim()
        if (lastName.isEmpty()) {
            etLastName.error = "Last name is required"
            isValid = false
        } else if (lastName.length < 2) {
            etLastName.error = "Last name must be at least 2 characters"
            isValid = false
        } else if (!lastName.matches(Regex("^[a-zA-Z\\s.-]+$"))) {
            etLastName.error = "Only letters, spaces, dots, and hyphens allowed"
            isValid = false
        } else {
            etLastName.error = null
        }

        val firstName = etFirstName.text.toString().trim()
        if (firstName.isEmpty()) {
            etFirstName.error = "First name is required"
            isValid = false
        } else if (firstName.length < 2) {
            etFirstName.error = "First name must be at least 2 characters"
            isValid = false
        } else if (!firstName.matches(Regex("^[a-zA-Z\\s.-]+$"))) {
            etFirstName.error = "Only letters, spaces, dots, and hyphens allowed"
            isValid = false
        } else {
            etFirstName.error = null
        }

        val username = etUsername.text.toString().trim()
        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            etUsername.error = "Username must be at least 3 characters"
            isValid = false
        } else if (username.length > 20) {
            etUsername.error = "Username must be less than 20 characters"
            isValid = false
        } else if (!username.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            etUsername.error = "Only letters, numbers, dots, and underscores allowed"
            isValid = false
        } else {
            etUsername.error = null
        }

        return isValid
    }

    override fun onResume() {
        super.onResume()
        val currentUser = sessionManager.getCurrentUser()
        currentUser?.let {
            this.currentUser = it
            updateProfileWithUserData(it)
            loadHighlights() // Reload highlights when returning to profile
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}