package com.example.pawsociety

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HighlightRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val highlightsCollection = db.collection("Highlights")

    suspend fun getUserHighlights(userId: String): List<Highlight> {
        return try {
            highlightsCollection
                .whereEqualTo("userId", userId)
                .get().await()
                .toObjects(Highlight::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveHighlight(highlight: Highlight): Boolean {
        return try {
            // we assume highlight gets a new ID, adjusting Highlight class structure may be needed,
            // using combination of name and userId as doc id if there is no highlightId right now
            val docId = "${highlight.userId}_${highlight.name.replace(" ", "_")}"
            highlightsCollection.document(docId).set(highlight).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
