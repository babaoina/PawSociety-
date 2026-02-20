package com.example.pawsociety

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val postRepository = PostRepository()
    private val userRepository = UserRepository()

    // Posts list
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    // UI State
    private val _uiState = MutableStateFlow<UiState<List<Post>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Post>>> = _uiState.asStateFlow()

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Current user
    private val _currentUser = MutableLiveData<AppUser?>()
    val currentUser: LiveData<AppUser?> = _currentUser

    // Favorite status map
    private val _favoriteStatus = MutableLiveData<Map<String, Boolean>>()
    val favoriteStatus: LiveData<Map<String, Boolean>> = _favoriteStatus

    init {
        loadCurrentUser()
        loadPosts()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUser()
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _isLoading.value = true

            postRepository.getAllPosts().collect { postList ->
                _posts.value = postList
                _uiState.value = UiState.Success(postList)
                _isLoading.value = false
                updateFavoriteStatus(postList)
            }
        }
    }

    private fun updateFavoriteStatus(posts: List<Post>) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.uid ?: return@launch
            val favoriteMap = mutableMapOf<String, Boolean>()
            
            posts.forEach { post ->
                favoriteMap[post.postId] = postRepository.isFavorite(userId, post.postId)
            }
            
            _favoriteStatus.value = favoriteMap
        }
    }

    fun createPost(
        petName: String,
        petType: String,
        status: String,
        description: String,
        location: String,
        reward: String,
        contactInfo: String,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch

            val post = Post(
                userName = user.username,
                userImageUrl = user.profileImageUrl,
                petName = petName,
                petType = petType,
                status = status,
                description = description,
                location = location,
                reward = reward,
                contactInfo = contactInfo,
                createdAt = com.google.firebase.Timestamp.now()
            )

            _isLoading.value = true
            val result = postRepository.createPost(getApplication(), post, imageUris)
            _isLoading.value = false

            if (result is Resource.Error) {
                _error.value = result.message
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = postRepository.deletePost(postId)
            _isLoading.value = false

            if (result is Resource.Error) {
                _error.value = result.message
            }
        }
    }

    fun toggleFavorite(post: Post, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.uid ?: return@launch

            val result = if (isCurrentlyLiked) {
                postRepository.removeFromFavorites(userId, post.postId)
            } else {
                postRepository.saveToFavorites(userId, post.postId)
            }

            // Update favorite status
            val currentMap = _favoriteStatus.value ?: emptyMap()
            _favoriteStatus.value = currentMap + (post.postId to !isCurrentlyLiked)
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            postRepository.addComment(postId, user.uid, user.username, text)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
