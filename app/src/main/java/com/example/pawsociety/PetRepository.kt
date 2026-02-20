package com.example.pawsociety

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class PetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storageRepo: StorageRepository = StorageRepository()
) {
    private val petsCollection = db.collection("Pets")

    fun getAllPets(): Flow<List<Pet>> = callbackFlow {
        val listenerRegistration = petsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pets = snapshot.toObjects(Pet::class.java)
                    trySend(pets)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun createPet(context: Context, pet: Pet, imageUri: Uri?): Resource<String> {
        return try {
            var uploadedUrl = ""
            if (imageUri != null) {
                when (val res = storageRepo.uploadImage(context, imageUri, "pet_images")) {
                    is Resource.Success -> uploadedUrl = res.data
                    is Resource.Error -> return Resource.Error(res.message)
                    else -> {}
                }
            }

            val finalPet = pet.copy(
                petId = UUID.randomUUID().toString(),
                ownerId = auth.currentUser?.uid ?: "",
                imageUrl = uploadedUrl
            )

            petsCollection.document(finalPet.petId).set(finalPet).await()
            Resource.Success(finalPet.petId)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create pet")
        }
    }
}
