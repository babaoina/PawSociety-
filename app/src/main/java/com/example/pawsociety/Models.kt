package com.example.pawsociety

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class AppUser(
    @DocumentId val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val isEmailVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Post(
    @DocumentId val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String = "",
    val petName: String = "",
    val petType: String = "",
    val status: String = "", // e.g., "Lost", "Found", "Adoptable"
    val description: String = "",
    val location: String = "",
    val reward: String = "",
    val contactInfo: String = "",
    val imageUrls: List<String> = emptyList(),
    val likes: List<String> = emptyList(), // UIDs of users who liked
    val comments: List<Comment> = emptyList(),
    val commentCount: Int = 0,
    val shares: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Comment(
    @DocumentId val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String = "",
    val text: String = "",
    val likes: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Favorite(
    @DocumentId val favoriteId: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Highlight(
    @DocumentId val highlightId: String = "",
    val userId: String = "",
    val name: String = "",
    val emoji: String = "",
    val color: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Notification(
    @DocumentId val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val relatedId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Message(
    @DocumentId val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class Conversation(
    @DocumentId val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null
)

data class Pet(
    @DocumentId val petId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val breed: String = "",
    val age: String = "",
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
