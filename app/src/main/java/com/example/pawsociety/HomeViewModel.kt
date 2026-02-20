package com.example.pawsociety

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val postRepository = PostRepository(MyApplication.instance)
    private val userRepository = UserRepository(MyApplication.instance)

    // UI States
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentUser = MutableLiveData<AppUser?>()
    val currentUser: LiveData<AppUser?> = _currentUser

    private val _favoriteStatus = MutableLiveData<Map<String, Boolean>>()
    val favoriteStatus: LiveData<Map<String, Boolean>> = _favoriteStatus

    init {
        _favoriteStatus.value = emptyMap()
        loadCurrentUser()
        loadPosts()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun loadPosts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val posts = postRepository.getAllPosts()
                _posts.value = posts
                _isLoading.value = false

                // Check favorite status for each post
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    val favMap = mutableMapOf<String, Boolean>()
                    for (post in posts) {
                        val isFav = postRepository.isFavorite(currentUser.uid, post.postId)
                        favMap[post.postId] = isFav
                    }
                    _favoriteStatus.value = favMap
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(post: Post, currentStatus: Boolean) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null) {
                val success = if (currentStatus) {
                    postRepository.removeFromFavorites(currentUser.uid, post.postId)
                } else {
                    postRepository.saveToFavorites(currentUser.uid, post.postId)
                }
                if (success) {
                    // Update local favorite status
                    val currentMap = _favoriteStatus.value?.toMutableMap() ?: mutableMapOf()
                    currentMap[post.postId] = !currentStatus
                    _favoriteStatus.value = currentMap
                }
            }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null) {
                val success = postRepository.addComment(postId, currentUser.uid, currentUser.username, text)
                if (success) {
                    // Refresh posts to show new comment
                    loadPosts()
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val success = postRepository.deletePost(postId)
            if (success) {
                loadPosts() // Refresh the list
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}