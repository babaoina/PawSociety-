package com.example.pawsociety

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationRepository {
    private val auth: FirebaseAuth by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("NotificationRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("NotificationRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }
    private val notificationsCollection = db.collection("Notifications")

    init {
        Log.d("NotificationRepository", "Initialized - Emulator: ${MyApplication.isConnectedToEmulator}")
    }

    /**
     * Create a notification for a user
     */
    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        relatedId: String = ""
    ): Resource<String> {
        return try {
            val notificationId = UUID.randomUUID().toString()
            val notification = Notification(
                notificationId = notificationId,
                userId = userId,
                title = title,
                message = message,
                type = type,
                relatedId = relatedId,
                isRead = false,
                createdAt = com.google.firebase.Timestamp.now()
            )

            notificationsCollection.document(notificationId).set(notification).await()
            Log.d("NotificationRepository", "Notification created for user: $userId")
            Resource.Success(notificationId)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Create notification failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to create notification")
        }
    }

    /**
     * Get user's notifications as Flow (real-time)
     */
    fun getUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        Log.d("NotificationRepository", "Setting up real-time listener for notifications")

        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepository", "Error listening to notifications: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                    Log.d("NotificationRepository", "Received ${notifications.size} notifications")
                    trySend(notifications)
                }
            }

        awaitClose {
            Log.d("NotificationRepository", "Removing notifications listener")
            listenerRegistration.remove()
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true).await()
            Log.d("NotificationRepository", "Notification marked as read: $notificationId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Mark as read failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to mark as read")
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            for (doc in notifications.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Log.d("NotificationRepository", "All notifications marked as read for user: $userId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Mark all as read failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to mark all as read")
        }
    }

    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Get unread count failed: ${e.message}", e)
            0
        }
    }

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Log.d("NotificationRepository", "Notification deleted: $notificationId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Delete failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete notification")
        }
    }

    // Notification type helpers
    companion object {
        const val TYPE_LIKE = "like"
        const val TYPE_COMMENT = "comment"
        const val TYPE_FOLLOW = "follow"
        const val TYPE_MESSAGE = "message"
        const val TYPE_POST = "post"
    }
}
