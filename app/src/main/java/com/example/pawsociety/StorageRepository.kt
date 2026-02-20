package com.example.pawsociety

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    private val storage: FirebaseStorage by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("StorageRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseStorage.getInstance()
    }

    init {
        Log.d("StorageRepository", "Initialized - Emulator connected: ${MyApplication.isConnectedToEmulator}")
    }

    /**
     * Upload image to Firebase Storage
     * @param context Android context
     * @param uri Image URI to upload
     * @param folder Folder path (e.g., "post_images/userId")
     * @return Resource with download URL or error
     */
    suspend fun uploadImage(context: Context, uri: Uri, folder: String): Resource<String> {
        return try {
            Log.d("StorageRepository", "Uploading to folder: $folder")
            Log.d("StorageRepository", "Source URI: $uri")

            // Validate image
            val validation = FileValidator.validateImage(context, uri)
            if (validation is Resource.Error) {
                return validation
            }

            // Open input stream
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Resource.Error("Could not open image stream")

            // Generate unique filename
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(folder).child(fileName)

            Log.d("StorageRepository", "Upload path: ${ref.path}")
            Log.d("StorageRepository", "Storage bucket: ${storage.reference.bucket}")

            // Upload using putStream (more reliable on emulators)
            val uploadTask = ref.putStream(inputStream).await()
            Log.d("StorageRepository", "Upload complete: ${uploadTask.bytesTransferred} bytes")

            // Get download URL
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d("StorageRepository", "✓ Upload successful! URL: $downloadUrl")

            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e("StorageRepository", "✗ UPLOAD FAILED: ${e.message}", e)
            Log.e("StorageRepository", "Check: 1) Emulators running  2) Storage rules  3) Network")
            Resource.Error(e.localizedMessage ?: "Image upload failed")
        }
    }

    /**
     * Delete image from Firebase Storage
     */
    suspend fun deleteImage(imageUrl: String): Resource<Unit> {
        return try {
            val ref = storage.getReferenceFromUrl(imageUrl)
            ref.delete().await()
            Log.d("StorageRepository", "Image deleted: $imageUrl")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Delete failed: ${e.message}", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete image")
        }
    }

    /**
     * Upload profile image
     */
    suspend fun uploadProfileImage(context: Context, uri: Uri, userId: String): Resource<String> {
        return uploadImage(context, uri, "profile_images/$userId")
    }
}
