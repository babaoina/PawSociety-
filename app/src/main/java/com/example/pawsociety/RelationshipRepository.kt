package com.example.pawsociety

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RelationshipRepository {
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("RelationshipRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }
    private val relCollection = db.collection("Relationships")

    init {
        Log.d("RelationshipRepository", "Initialized - Emulator: ${MyApplication.isConnectedToEmulator}")
    }

    suspend fun followUser(followerId: String, followedId: String): Resource<Unit> {
        return try {
            val docId = "${followerId}_${followedId}"
            val data = mapOf(
                "followerId" to followerId,
                "followedId" to followedId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            relCollection.document(docId).set(data).await()

            // Send notification to followed user
            val notificationRepo = NotificationRepository()
            notificationRepo.createNotification(
                userId = followedId,
                title = "New Follower",
                message = "Someone started following you",
                type = NotificationRepository.TYPE_FOLLOW,
                relatedId = followerId
            )

            Log.d("RelationshipRepository", "User $followerId followed $followedId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("RelationshipRepository", "Follow failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to follow user")
        }
    }

    suspend fun unfollowUser(followerId: String, followedId: String): Resource<Unit> {
        return try {
            val docId = "${followerId}_${followedId}"
            relCollection.document(docId).delete().await()
            Log.d("RelationshipRepository", "User $followerId unfollowed $followedId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("RelationshipRepository", "Unfollow failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to unfollow user")
        }
    }

    fun isFollowing(followerId: String, followedId: String): Flow<Boolean> = callbackFlow {
        val docId = "${followerId}_${followedId}"
        val listener = relCollection.document(docId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.exists())
            }
        }
        awaitClose { listener.remove() }
    }

    fun getFollowerCount(userId: String): Flow<Int> = callbackFlow {
        val listener = relCollection.whereEqualTo("followedId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.size())
                }
            }
        awaitClose { listener.remove() }
    }

    fun getFollowingCount(userId: String): Flow<Int> = callbackFlow {
        val listener = relCollection.whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.size())
                }
            }
        awaitClose { listener.remove() }
    }

    fun getFollowers(userId: String): Flow<List<String>> = callbackFlow {
        val listener = relCollection.whereEqualTo("followedId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val followerIds = snapshot.documents.mapNotNull {
                        it.getString("followerId")
                    }
                    trySend(followerIds)
                }
            }
        awaitClose { listener.remove() }
    }
}
