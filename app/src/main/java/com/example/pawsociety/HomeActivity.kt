package com.example.pawsociety

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : BaseNavigationActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var postsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private var currentUser: AppUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Initialize views
        postsContainer = findViewById(R.id.posts_container)
        emptyState = findViewById(R.id.empty_state)
        progressBar = findViewById(R.id.progress_bar)

        // Setup observers
        setupObservers()

        // Notifications button
        findViewById<ImageView>(R.id.btn_notifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun setupObservers() {
        // Observe current user
        viewModel.currentUser.observe(this, Observer { user ->
            currentUser = user
            if (user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        })

        // Observe posts
        viewModel.posts.observe(this, Observer { posts ->
            displayPosts(posts)
        })

        // Observe loading state
        viewModel.isLoading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Observe errors
        viewModel.error.observe(this, Observer { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })
    }

    private fun displayPosts(posts: List<Post>) {
        postsContainer.removeAllViews()

        if (posts.isEmpty()) {
            postsContainer.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        postsContainer.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        for (post in posts) {
            createPostView(post)
        }
    }

    private fun createPostView(post: Post) {
        val inflater = LayoutInflater.from(this)
        val postView = inflater.inflate(R.layout.item_post, postsContainer, false)

        val userNameText = postView.findViewById<TextView>(R.id.post_user_name)
        val locationText = postView.findViewById<TextView>(R.id.post_location)
        val petNameText = postView.findViewById<TextView>(R.id.post_pet_name)
        val statusText = postView.findViewById<TextView>(R.id.post_status)
        val rewardText = postView.findViewById<TextView>(R.id.post_reward)
        val descriptionText = postView.findViewById<TextView>(R.id.post_description)
        val contactText = postView.findViewById<TextView>(R.id.post_contact)
        val dateText = postView.findViewById<TextView>(R.id.post_date)
        val btnMore = postView.findViewById<TextView>(R.id.btn_more)
        val btnLike = postView.findViewById<ImageView>(R.id.btn_like)
        val btnComment = postView.findViewById<TextView>(R.id.btn_comment)
        val btnShare = postView.findViewById<TextView>(R.id.btn_share)

        // Set post data
        userNameText.text = post.userName
        locationText.text = post.location
        petNameText.text = post.petName
        statusText.text = post.status
        descriptionText.text = post.description
        contactText.text = post.contactInfo
        dateText.text = post.createdAt

        // Set status color and reward
        when (post.status) {
            "Lost" -> {
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                // Show formatted reward for Lost posts
                if (post.reward.isNotEmpty()) {
                    val formattedReward = formatReward(post.reward)

                    // Check if original exceeded limit
                    val digitsOnly = post.reward.replace("[^0-9]".toRegex(), "")
                    val originalNumber = if (digitsOnly.isNotEmpty()) digitsOnly.toLong() else 0

                    rewardText.text = if (originalNumber > 1000000) {
                        "💰 Reward: $formattedReward (max limit)"
                    } else {
                        "💰 Reward: $formattedReward"
                    }
                    rewardText.visibility = View.VISIBLE
                } else {
                    rewardText.visibility = View.GONE
                }
            }
            "Found" -> {
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                rewardText.visibility = View.GONE
            }
            "Adoption" -> {
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                rewardText.visibility = View.GONE
            }
        }

        // Observe favorite status for this post
        viewModel.favoriteStatus.observe(this, Observer { favMap ->
            val isFav = favMap[post.postId]
            if (isFav != null && isFav) {
                btnLike.setColorFilter(Color.parseColor("#B88B4A"), PorterDuff.Mode.SRC_ATOP)
                btnLike.tag = "liked"
            } else {
                btnLike.setColorFilter(Color.parseColor("#FF6B35"), PorterDuff.Mode.SRC_ATOP)
                btnLike.tag = "unliked"
            }
        })

        // More options (3 dots) click listener
        btnMore.setOnClickListener {
            showPostOptions(post, btnLike)
        }

        // Like button click listener
        btnLike.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Please login to like posts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isLiked = btnLike.tag == "liked"
            viewModel.toggleFavorite(post, isLiked)
        }

        // Comment button
        btnComment.setOnClickListener {
            showCommentsDialog(post)
        }

        // Share button
        btnShare.setOnClickListener {
            sharePost(post)
        }

        postsContainer.addView(postView)
    }

    // Format reward with commas and limit to 1 million
    private fun formatReward(reward: String): String {
        return try {
            // Remove any non-digit characters first
            val digitsOnly = reward.replace("[^0-9]".toRegex(), "")

            if (digitsOnly.isEmpty()) return reward

            // Convert to number
            var number = digitsOnly.toLong()

            // Limit to 1 million (1,000,000)
            if (number > 1000000) {
                number = 1000000
            }

            // Format with commas for thousands
            String.format("%,d", number)
        } catch (e: Exception) {
            // If parsing fails, return the original
            reward
        }
    }

    private fun showCommentsDialog(post: Post) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_comments, null)

        val postIcon = dialogView.findViewById<TextView>(R.id.comment_post_icon)
        val postUser = dialogView.findViewById<TextView>(R.id.comment_post_user)
        val postCaption = dialogView.findViewById<TextView>(R.id.comment_post_caption)
        val commentsContainer = dialogView.findViewById<LinearLayout>(R.id.comments_container)
        val btnClose = dialogView.findViewById<TextView>(R.id.btn_close_comments)
        val commentInput = dialogView.findViewById<EditText>(R.id.comment_input)
        val btnPostComment = dialogView.findViewById<TextView>(R.id.btn_post_comment)

        val emoji = when {
            post.petType.contains("dog", ignoreCase = true) -> "🐶"
            post.petType.contains("cat", ignoreCase = true) -> "🐱"
            post.petType.contains("bird", ignoreCase = true) -> "🐦"
            post.petType.contains("rabbit", ignoreCase = true) -> "🐰"
            post.petType.contains("fish", ignoreCase = true) -> "🐟"
            else -> "🐾"
        }

        postIcon.text = emoji
        postUser.text = post.userName
        postCaption.text = "${post.petName} • ${post.status}\n${post.description}"

        commentsContainer.removeAllViews()
        loadComments(commentsContainer, post.postId)

        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnPostComment.setOnClickListener {
            val text = commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                if (currentUser == null) {
                    Toast.makeText(this, "Please login to comment", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.addComment(post.postId, text)
                commentInput.text.clear()
                Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()
                // Refresh comments
                commentsContainer.removeAllViews()
                loadComments(commentsContainer, post.postId)
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun loadComments(container: LinearLayout, postId: String) {
        val comments = UserDatabase.getCommentsForPost(this, postId)

        if (comments.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            emptyView.gravity = android.view.Gravity.CENTER
            emptyView.text = "No comments yet\nBe the first to comment!"
            emptyView.setTextColor(Color.parseColor("#999999"))
            emptyView.textSize = 14f
            emptyView.setPadding(0, 50, 0, 50)
            container.addView(emptyView)
            return
        }

        for (comment in comments) {
            addCommentView(container, comment)
        }
    }

    private fun addCommentView(container: LinearLayout, comment: Comment) {
        val commentView = layoutInflater.inflate(R.layout.item_comment, container, false)

        val userIcon = commentView.findViewById<TextView>(R.id.comment_user_icon)
        val userName = commentView.findViewById<TextView>(R.id.comment_user_name)
        val commentText = commentView.findViewById<TextView>(R.id.comment_text)
        val commentTime = commentView.findViewById<TextView>(R.id.comment_time)
        val likeButton = commentView.findViewById<TextView>(R.id.comment_like_button)
        val likeCount = commentView.findViewById<TextView>(R.id.comment_like_count)

        val firstLetter = if (comment.userName.isNotEmpty()) {
            comment.userName.first().toString().uppercase()
        } else {
            "?"
        }
        userIcon.text = firstLetter

        val colors = listOf("#7A4F2B", "#B88B4A", "#4CAF50", "#2196F3", "#FF9800")
        val colorIndex = Math.abs(comment.userId.hashCode()) % colors.size
        userIcon.setBackgroundColor(Color.parseColor(colors[colorIndex]))

        userName.text = comment.userName
        commentText.text = comment.text

        val timeAgo = getTimeAgo(comment.createdAt)
        commentTime.text = timeAgo

        val isLiked = currentUser != null && comment.likes.contains(currentUser!!.uid)
        if (isLiked) {
            likeButton.setTextColor(Color.parseColor("#FF6B35"))
            likeButton.tag = "liked"
        } else {
            likeButton.setTextColor(Color.parseColor("#999999"))
            likeButton.tag = "unliked"
        }

        likeCount.text = comment.likes.size.toString()

        likeButton.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Please login to like comments", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = UserDatabase.likeComment(this, comment.postId, comment.commentId, currentUser!!.uid)

            if (success) {
                val liked = likeButton.tag == "liked"
                if (liked) {
                    likeButton.setTextColor(Color.parseColor("#999999"))
                    likeButton.tag = "unliked"
                    likeCount.text = (comment.likes.size - 1).toString()
                } else {
                    likeButton.setTextColor(Color.parseColor("#FF6B35"))
                    likeButton.tag = "liked"
                    likeCount.text = (comment.likes.size + 1).toString()
                }
            }
        }

        container.addView(commentView)
    }

    private fun getTimeAgo(dateTime: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateTime)
            val now = Date()

            if (date != null) {
                val diff = now.time - date.time
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24

                when {
                    days > 0 -> "${days}d ago"
                    hours > 0 -> "${hours}h ago"
                    minutes > 0 -> "${minutes}m ago"
                    else -> "Just now"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun showPostOptions(post: Post, btnLike: ImageView) {
        val dialogView = layoutInflater.inflate(R.layout.post_menu_instagram, null)

        val menuUsername = dialogView.findViewById<TextView>(R.id.menu_username)
        val menuPostInfo = dialogView.findViewById<TextView>(R.id.menu_post_info)
        val btnClose = dialogView.findViewById<TextView>(R.id.btn_close)
        val optionRemix = dialogView.findViewById<LinearLayout>(R.id.option_remix)
        val optionQr = dialogView.findViewById<LinearLayout>(R.id.option_qr)
        val optionAddFavorites = dialogView.findViewById<LinearLayout>(R.id.option_add_favorites)
        val optionUnfollow = dialogView.findViewById<LinearLayout>(R.id.option_unfollow)
        val optionWhySeeing = dialogView.findViewById<LinearLayout>(R.id.option_why_seeing)
        val optionHide = dialogView.findViewById<LinearLayout>(R.id.option_hide)
        val optionAboutAccount = dialogView.findViewById<LinearLayout>(R.id.option_about_account)
        val optionReport = dialogView.findViewById<LinearLayout>(R.id.option_report)
        val optionDelete = dialogView.findViewById<LinearLayout>(R.id.option_delete)

        menuUsername.text = post.userName
        menuPostInfo.text = "${post.petName} • ${post.status}"

        if (currentUser != null && post.userId == currentUser!!.uid) {
            optionDelete.visibility = View.VISIBLE
        }

        val favoriteText = dialogView.findViewById<TextView>(R.id.tv_favorite_text)

        // Get favorite status from ViewModel
        val favMap = viewModel.favoriteStatus.value
        val isFav = favMap?.get(post.postId) ?: false
        favoriteText.text = if (isFav) "Remove from favorites" else "Add to favorites"

        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setGravity(android.view.Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(null)
            decorView.setPadding(0, 0, 0, 0)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            attributes?.dimAmount = 0.5f
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialogView.setPadding(0, 0, 0, 0)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        optionRemix.setOnClickListener {
            Toast.makeText(this, "Remix feature coming soon", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        optionQr.setOnClickListener {
            Toast.makeText(this, "QR code feature coming soon", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        optionAddFavorites.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }
            val isLiked = btnLike.tag == "liked"
            viewModel.toggleFavorite(post, isLiked)
            dialog.dismiss()
        }

        optionUnfollow.setOnClickListener {
            Toast.makeText(this, "Unfollowed ${post.userName}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        optionWhySeeing.setOnClickListener {
            showWhySeeingDialog(post)
            dialog.dismiss()
        }

        optionHide.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hide this post?")
                .setMessage("You won't see this post again in your feed.")
                .setPositiveButton("Hide") { _, _ ->
                    Toast.makeText(this, "Post hidden", Toast.LENGTH_SHORT).show()
                    showUndoSnackbar(post)
                }
                .setNegativeButton("Cancel", null)
                .show()
            dialog.dismiss()
        }

        optionAboutAccount.setOnClickListener {
            showAboutAccountDialog(post)
            dialog.dismiss()
        }

        optionReport.setOnClickListener {
            showReportDialog(post)
            dialog.dismiss()
        }

        optionDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deletePost(post.postId)
                }
                .setNegativeButton("Cancel", null)
                .show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUndoSnackbar(post: Post) {
        val view = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(view, "Post hidden", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo") {
            Toast.makeText(this, "Post restored", Toast.LENGTH_SHORT).show()
        }
        snackbar.show()
    }

    private fun showWhySeeingDialog(post: Post) {
        val message = """
            You're seeing this post because:
            
            • You follow @${post.userName}
            • This post has high engagement
            • Similar content you've liked before
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Why you're seeing this post")
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showAboutAccountDialog(post: Post) {
        val user = UserDatabase.getUserById(this, post.userId)
        val message = """
            About @${post.userName}
            
            📝 Bio: ${user?.bio ?: "No bio"}
            📍 Location: ${user?.location ?: "Not set"}
            📅 Joined: ${user?.createdAt ?: "Unknown"}
            🐾 Posts: ${UserDatabase.getAllPosts(this).count { it.userId == post.userId }}
            
            This account posts about pets and rescue stories.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About this account")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showReportDialog(post: Post) {
        val options = arrayOf("Spam", "Inappropriate", "False information", "Scam", "Harassment", "Other")

        AlertDialog.Builder(this)
            .setTitle("Report Post")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Reported as: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostDetails(post: Post) {
        val rewardText = if (post.reward.isNotEmpty()) {
            val digitsOnly = post.reward.replace("[^0-9]".toRegex(), "")
            val originalNumber = if (digitsOnly.isNotEmpty()) digitsOnly.toLong() else 0
            val formattedReward = formatReward(post.reward)

            if (originalNumber > 1000000) {
                "💰 Reward: $formattedReward (max limit)"
            } else {
                "💰 Reward: $formattedReward"
            }
        } else {
            "No reward"
        }

        val details = """
        🐾 ${post.petName}
        👤 By: ${post.userName}
        📍 ${post.location}
        🔍 Status: ${post.status}
        ${rewardText}
        📞 ${post.contactInfo}
        
        ${post.description}
        
        🕒 ${post.createdAt}
    """.trimIndent()

        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
    }

    private fun sharePost(post: Post) {
        val shareText = """
            Check out this pet on PawSociety!
            
            ${post.petName} - ${post.status}
            Posted by: ${post.userName}
            Location: ${post.location}
            
            ${post.description}
            
            Contact: ${post.contactInfo}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, "Share post"))
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to home
        viewModel.loadPosts()
        viewModel.loadCurrentUser()
    }
}