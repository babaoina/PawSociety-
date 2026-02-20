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

class ChatRepository {
    private val auth: FirebaseAuth by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("ChatRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("ChatRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }
    private val conversationsCollection = db.collection("Conversations")
    private val messagesCollection = db.collection("Messages")

    init {
        Log.d("ChatRepository", "Initialized - Emulator connected: ${MyApplication.isConnectedToEmulator}")
    }

    /**
     * Get or create conversation between two users
     */
    suspend fun getOrCreateConversation(otherUserId: String): Resource<String> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            // Check if conversation exists
            val existingConv = conversationsCollection
                .whereEqualTo("participants", listOf(currentUserId, otherUserId))
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (existingConv != null) {
                Log.d("ChatRepository", "Found existing conversation: ${existingConv.id}")
                return Resource.Success(existingConv.id)
            }

            // Create new conversation
            val conversationId = UUID.randomUUID().toString()
            val conversation = Conversation(
                conversationId = conversationId,
                participants = listOf(currentUserId, otherUserId),
                lastMessage = "",
                lastMessageTimestamp = com.google.firebase.Timestamp.now()
            )

            conversationsCollection.document(conversationId).set(conversation).await()
            Log.d("ChatRepository", "Created new conversation: $conversationId")

            Resource.Success(conversationId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Get/create conversation failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to get conversation")
        }
    }

    /**
     * Send message
     */
    suspend fun sendMessage(conversationId: String, receiverId: String, text: String): Resource<String> {
        return try {
            val senderId = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            val message = Message(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                text = text,
                createdAt = com.google.firebase.Timestamp.now()
            )

            // Add message to messages collection
            messagesCollection.document(message.messageId).set(message).await()

            // Update conversation's last message
            conversationsCollection.document(conversationId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to com.google.firebase.Timestamp.now()
                )
            ).await()

            // Send notification to receiver
            val notificationRepo = NotificationRepository()
            notificationRepo.createNotification(
                userId = receiverId,
                title = "New Message",
                message = text,
                type = NotificationRepository.TYPE_MESSAGE,
                relatedId = conversationId
            )

            Log.d("ChatRepository", "Message sent in conversation: $conversationId")
            Resource.Success(message.messageId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Send message failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to send message")
        }
    }

    /**
     * Get messages for a conversation as Flow (real-time)
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        Log.d("ChatRepository", "Setting up real-time listener for messages in: $conversationId")

        val listenerRegistration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to messages: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    Log.d("ChatRepository", "Received ${messages.size} messages")
                    trySend(messages)
                }
            }

        awaitClose {
            Log.d("ChatRepository", "Removing messages listener")
            listenerRegistration.remove()
        }
    }

    /**
     * Get user's conversations as Flow (real-time)
     */
    fun getUserConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow

        Log.d("ChatRepository", "Setting up real-time listener for conversations")

        val listenerRegistration = conversationsCollection
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to conversations: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val conversations = snapshot.toObjects(Conversation::class.java)
                    Log.d("ChatRepository", "Received ${conversations.size} conversations")
                    trySend(conversations)
                }
            }

        awaitClose {
            Log.d("ChatRepository", "Removing conversations listener")
            listenerRegistration.remove()
        }
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: String): Resource<Unit> {
        return try {
            conversationsCollection.document(conversationId).delete().await()
            Log.d("ChatRepository", "Conversation deleted: $conversationId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Delete conversation failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete conversation")
        }
    }
}
