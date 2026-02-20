package com.example.pawsociety

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository {
    private val auth: FirebaseAuth by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("PostRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("PostRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }
    private val storageRepo: StorageRepository by lazy { StorageRepository() }
    private val postsCollection = db.collection("Posts")
    private val favoritesCollection = db.collection("Favorites")

    init {
        Log.d("PostRepository", "Initialized - Emulator connected: ${MyApplication.isConnectedToEmulator}")
    }

    /**
     * Get all posts as a Flow with real-time updates
     */
    fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        Log.d("PostRepository", "Setting up real-time listener for Posts")

        val listenerRegistration = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error listening to posts: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    Log.d("PostRepository", "Received ${posts.size} posts")
                    trySend(posts)
                }
            }

        awaitClose {
            Log.d("PostRepository", "Removing posts listener")
            listenerRegistration.remove()
        }
    }

    /**
     * Create a new post with image uploads
     */
    suspend fun createPost(context: Context, post: Post, imageUris: List<Uri>): Resource<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Resource.Error("User not authenticated")
            val postId = UUID.randomUUID().toString()

            Log.d("PostRepository", "========== START POST CREATION ==========")
            Log.d("PostRepository", "Post ID: $postId")
            Log.d("PostRepository", "User ID: $userId")
            Log.d("PostRepository", "Number of images: ${imageUris.size}")

            val uploadedUrls = mutableListOf<String>()
            for ((index, uri) in imageUris.withIndex()) {
                val folderPath = "post_images/$userId"
                Log.d("PostRepository", "Uploading image ${index + 1}/${imageUris.size} to: $folderPath")

                when (val res = storageRepo.uploadImage(context, uri, folderPath)) {
                    is Resource.Success -> {
                        uploadedUrls.add(res.data)
                        Log.d("PostRepository", "✓ Image ${index + 1} uploaded: ${res.data}")
                    }
                    is Resource.Error -> {
                        Log.e("PostRepository", "✗ Image upload failed: ${res.message}")
                        return Resource.Error("Image upload failed: ${res.message}")
                    }
                    else -> {}
                }
            }

            val finalPost = post.copy(
                postId = postId,
                userId = userId,
                imageUrls = uploadedUrls
            )

            Log.d("PostRepository", "Writing post to Firestore: Posts/$postId")
            postsCollection.document(postId).set(finalPost).await()

            Log.d("PostRepository", "========== POST CREATED SUCCESSFULLY ==========")
            Log.d("PostRepository", "Post ID: $postId")
            Log.d("PostRepository", "Image URLs: ${uploadedUrls.size}")

            Resource.Success(postId)
        } catch (e: Exception) {
            Log.e("PostRepository", "========== POST CREATION FAILED ==========")
            Log.e("PostRepository", "Error: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to create post")
        }
    }

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            Log.d("PostRepository", "Deleting post: $postId")
            postsCollection.document(postId).delete().await()
            Log.d("PostRepository", "Post deleted successfully")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Delete failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete post")
        }
    }

    /**
     * Save post to favorites
     */
    suspend fun saveToFavorites(userId: String, postId: String): Resource<Unit> {
        return try {
            val fav = Favorite(
                favoriteId = "${userId}_${postId}",
                userId = userId,
                postId = postId
            )
            favoritesCollection.document(fav.favoriteId).set(fav).await()
            Log.d("PostRepository", "Added to favorites: $postId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Favorite failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to save to favorites")
        }
    }

    /**
     * Remove from favorites
     */
    suspend fun removeFromFavorites(userId: String, postId: String): Resource<Unit> {
        return try {
            favoritesCollection.document("${userId}_${postId}").delete().await()
            Log.d("PostRepository", "Removed from favorites: $postId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Remove favorite failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to remove from favorites")
        }
    }

    /**
     * Check if post is favorite
     */
    suspend fun isFavorite(userId: String, postId: String): Boolean {
        return try {
            val doc = favoritesCollection.document("${userId}_${postId}").get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e("PostRepository", "Check favorite failed: ${e.message}", e)
            false
        }
    }

    /**
     * Get user's favorite posts
     */
    fun getUserFavorites(userId: String): Flow<List<String>> = callbackFlow {
        val listenerRegistration = favoritesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val postIds = snapshot.documents.mapNotNull { it.getString("postId") }
                    trySend(postIds)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Add comment to post
     */
    suspend fun addComment(postId: String, userId: String, userName: String, text: String): Resource<String> {
        return try {
            val comment = Comment(
                commentId = UUID.randomUUID().toString(),
                postId = postId,
                userId = userId,
                userName = userName,
                text = text
            )

            // Add comment to subcollection
            postsCollection.document(postId).collection("Comments")
                .document(comment.commentId).set(comment).await()

            // Update comment count on post
            val postRef = postsCollection.document(postId)
            val postDoc = postRef.get().await()
            val post = postDoc.toObject(Post::class.java)

            if (post != null) {
                val newCount = (post.commentCount + 1)
                postRef.update("commentCount", newCount).await()
            }

            // Send notification to post owner
            if (post != null && post.userId != userId) {
                val notificationRepo = NotificationRepository()
                notificationRepo.createNotification(
                    userId = post.userId,
                    title = "New Comment",
                    message = "$userName commented on your post",
                    type = NotificationRepository.TYPE_COMMENT,
                    relatedId = postId
                )
            }

            Log.d("PostRepository", "Comment added to post: $postId")
            Resource.Success(comment.commentId)
        } catch (e: Exception) {
            Log.e("PostRepository", "Add comment failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to add comment")
        }
    }

    /**
     * Get comments for a post
     */
    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = postsCollection.document(postId)
            .collection("Comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(comments)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Toggle like on post
     */
    suspend fun toggleLike(postId: String, userId: String): Resource<Boolean> {
        return try {
            val postDoc = postsCollection.document(postId).get().await()
            val post = postDoc.toObject(Post::class.java) ?: return Resource.Error("Post not found")

            val updatedLikes = post.likes.toMutableList()
            val isLiked = if (updatedLikes.contains(userId)) {
                updatedLikes.remove(userId)
                false
            } else {
                updatedLikes.add(userId)
                true
            }

            // Update likes array
            postsCollection.document(postId).update("likes", updatedLikes).await()

            // Send notification to post owner if liked
            if (isLiked && post.userId != userId) {
                val notificationRepo = NotificationRepository()
                notificationRepo.createNotification(
                    userId = post.userId,
                    title = "New Like",
                    message = "Someone liked your post",
                    type = NotificationRepository.TYPE_LIKE,
                    relatedId = postId
                )
            }

            Log.d("PostRepository", "Like toggled on post $postId: $isLiked")
            Resource.Success(isLiked)
        } catch (e: Exception) {
            Log.e("PostRepository", "Toggle like failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to toggle like")
        }
    }
}
