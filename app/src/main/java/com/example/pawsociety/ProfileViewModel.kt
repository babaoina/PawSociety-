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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val storageRepository = StorageRepository()
    private val postRepository = PostRepository()

    // User data
    private val _user = MutableLiveData<AppUser?>()
    val user: LiveData<AppUser?> = _user

    // UI State
    private val _uiState = MutableStateFlow<UiState<AppUser>>(UiState.Loading)
    val uiState: StateFlow<UiState<AppUser>> = _uiState.asStateFlow()

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // User's posts
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    // User's favorite posts
    private val _favoritePosts = MutableLiveData<List<Post>>()
    val favoritePosts: LiveData<List<Post>> = _favoritePosts

    // Highlights
    private val _highlights = MutableLiveData<List<Highlight>>()
    val highlights: LiveData<List<Highlight>> = _highlights

    init {
        loadUserProfile()
        loadHighlights()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _isLoading.value = true

            _user.value = userRepository.getCurrentUser()

            _uiState.value = if (_user.value != null) {
                UiState.Success(_user.value!!)
            } else {
                UiState.Error("User not found")
            }

            _isLoading.value = false
        }
    }

    fun updateProfile(user: AppUser) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _isLoading.value = true

            val result = userRepository.updateUser(user)

            if (result is Resource.Success) {
                _user.value = user
                _uiState.value = UiState.Success(user)
            } else if (result is Resource.Error) {
                _error.value = result.message
                _uiState.value = UiState.Error(result.message)
            }

            _isLoading.value = false
        }
    }

    fun updateUserProfile(
        fullName: String,
        username: String,
        bio: String,
        phone: String
    ) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch

            _uiState.value = UiState.Loading
            _isLoading.value = true

            val updatedUser = currentUser.copy(
                fullName = fullName,
                username = username,
                bio = bio,
                phone = phone
            )

            val result = userRepository.updateUser(updatedUser)

            if (result is Resource.Success) {
                _user.value = updatedUser
                _uiState.value = UiState.Success(updatedUser)
            } else if (result is Resource.Error) {
                _error.value = result.message
                _uiState.value = UiState.Error(result.message)
            }

            _isLoading.value = false
        }
    }

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch

            _uiState.value = UiState.Loading
            _isLoading.value = true

            val result = storageRepository.uploadProfileImage(getApplication(), uri, currentUser.uid)

            if (result is Resource.Success) {
                val updatedUser = currentUser.copy(profileImageUrl = result.data)
                val updateResult = userRepository.updateUser(updatedUser)

                if (updateResult is Resource.Success) {
                    _user.value = updatedUser
                    _uiState.value = UiState.Success(updatedUser)
                } else {
                    _error.value = "Failed to update profile"
                }
            } else if (result is Resource.Error) {
                _error.value = result.message
            }

            _isLoading.value = false
        }
    }

    fun loadUserPosts() {
        viewModelScope.launch {
            val userId = _user.value?.uid ?: return@launch

            postRepository.getAllPosts().collect { allPosts ->
                val userPostsList = allPosts.filter { it.userId == userId }
                _userPosts.value = userPostsList
            }
        }
    }

    fun loadFavoritePosts() {
        viewModelScope.launch {
            val userId = _user.value?.uid ?: return@launch

            postRepository.getUserFavorites(userId).collect { favoritePostIds ->
                postRepository.getAllPosts().collect { allPosts ->
                    val favorites = allPosts.filter { it.postId in favoritePostIds }
                    _favoritePosts.value = favorites
                }
            }
        }
    }

    fun saveHighlight(highlight: Highlight) {
        viewModelScope.launch {
            val updatedHighlights = (_highlights.value ?: emptyList()).toMutableList()
            updatedHighlights.add(highlight)
            _highlights.value = updatedHighlights
        }
    }

    fun loadHighlights() {
        // Load highlights from Firestore if needed
        // For now, use empty list
        _highlights.value = emptyList()
    }

    fun refreshData() {
        loadUserProfile()
        loadUserPosts()
        loadFavoritePosts()
    }

    fun clearError() {
        _error.value = null
    }
}
