package com.example.pawsociety

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth: FirebaseAuth by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("UserRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("UserRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }
    private val usersCollection = db.collection("Users")

    init {
        Log.d("UserRepository", "Initialized - Emulator connected: ${MyApplication.isConnectedToEmulator}")
    }

    /**
     * Get current user from Firestore
     */
    suspend fun getCurrentUser(): AppUser? {
        val firebaseUser = auth.currentUser ?: return null

        Log.d("UserRepository", "Getting user: ${firebaseUser.uid}")

        return try {
            val doc = usersCollection.document(firebaseUser.uid).get().await()

            if (doc.exists()) {
                Log.d("UserRepository", "User document found")
                doc.toObject(AppUser::class.java)
            } else {
                Log.d("UserRepository", "User document missing, creating...")
                // Auto-provision a minimal user document if missing
                val minimal = AppUser(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "",
                    username = (firebaseUser.email ?: firebaseUser.phoneNumber ?: firebaseUser.uid).substringBefore("@"),
                    email = firebaseUser.email ?: "",
                    phone = firebaseUser.phoneNumber ?: "",
                    isEmailVerified = true // Auto-verified in emulator
                )
                usersCollection.document(firebaseUser.uid).set(minimal).await()
                Log.d("UserRepository", "User document created")
                minimal
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user: ${e.message}", e)
            null
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): AppUser? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(AppUser::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Get user failed: ${e.message}", e)
            null
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUser(user: AppUser): Resource<Unit> {
        return try {
            Log.d("UserRepository", "Updating user: ${user.uid}")
            usersCollection.document(user.uid).set(user).await()
            Log.d("UserRepository", "User updated successfully")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Update failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to update user")
        }
    }

    /**
     * Get all users as Flow
     */
    fun getAllUsers(): Flow<List<AppUser>> = callbackFlow {
        val listenerRegistration = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error listening to users: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(AppUser::class.java)
                    Log.d("UserRepository", "Received ${users.size} users")
                    trySend(users)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Get user by username
     */
    suspend fun getUserByUsername(username: String): AppUser? {
        return try {
            val query = usersCollection.whereEqualTo("username", username).limit(1).get().await()
            query.documents.firstOrNull()?.toObject(AppUser::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Get by username failed: ${e.message}", e)
            null
        }
    }

    /**
     * Search users by username
     */
    fun searchUsers(query: String): Flow<List<AppUser>> = callbackFlow {
        val listenerRegistration = usersCollection
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(AppUser::class.java)
                    trySend(users)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}
