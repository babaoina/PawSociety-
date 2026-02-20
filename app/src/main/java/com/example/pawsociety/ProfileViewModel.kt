package com.example.pawsociety

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository(MyApplication.instance)
    private val postRepository = PostRepository(MyApplication.instance)

    private val _user = MutableLiveData<AppUser?>()
    val user: LiveData<AppUser?> = _user

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _favoritePosts = MutableLiveData<List<Post>>()
    val favoritePosts: LiveData<List<Post>> = _favoritePosts

    private val _highlights = MutableLiveData<List<ProfileActivity.Highlight>>()
    val highlights: LiveData<List<ProfileActivity.Highlight>> = _highlights

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _isLoading.value = false
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            _user.value = user
            user?.let {
                loadUserPosts(it.uid)
                loadHighlights(it.uid)
            }
        }
    }

    private fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            val allPosts = postRepository.getAllPosts()
            val userPostsList = allPosts.filter { post -> post.userId == userId }
            _userPosts.value = userPostsList

            // Load favorites
            loadFavoritePosts(userId, allPosts)
        }
    }

    private fun loadFavoritePosts(userId: String, allPosts: List<Post>) {
        viewModelScope.launch {
            val favoritePostsList = mutableListOf<Post>()
            for (post in allPosts) {
                val isFav = postRepository.isFavorite(userId, post.postId)
                if (isFav) {
                    favoritePostsList.add(post)
                }
            }
            _favoritePosts.value = favoritePostsList
        }
    }

    fun updateProfile(updatedUser: AppUser) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = userRepository.updateUser(updatedUser)
            if (success) {
                _user.value = updatedUser
            }
            _isLoading.value = false
        }
    }

    fun saveHighlights(highlights: List<ProfileActivity.Highlight>) {
        viewModelScope.launch {
            val user = _user.value
            user?.let {
                UserDatabase.saveUserHighlights(MyApplication.instance, it.uid, highlights)
                loadHighlights(it.uid)
            }
        }
    }

    private fun loadHighlights(userId: String) {
        val savedHighlights = UserDatabase.getUserHighlights(MyApplication.instance, userId)
        _highlights.value = savedHighlights
    }

    fun refreshData() {
        loadUserData()
    }

    fun clearError() {
        // No error state in this ViewModel yet
    }
}