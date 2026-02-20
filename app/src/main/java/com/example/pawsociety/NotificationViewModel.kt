package com.example.pawsociety

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationRepository = NotificationRepository()
    private val userRepository = UserRepository()

    // Notifications list
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    // Unread count
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // UI State
    private val _uiState = MutableStateFlow<UiState<List<Notification>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Notification>>> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.uid ?: return@launch
            
            _uiState.value = UiState.Loading
            _isLoading.value = true

            notificationRepository.getUserNotifications(userId).collect { notificationList ->
                _notifications.value = notificationList
                _uiState.value = UiState.Success(notificationList)
                _isLoading.value = false
                
                // Update unread count
                _unreadCount.value = notificationList.count { !it.isRead }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(
                userRepository.getCurrentUser()?.uid ?: return@launch
            )
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun getUnreadCount() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.uid ?: return@launch
            _unreadCount.value = notificationRepository.getUnreadCount(userId)
        }
    }
}
