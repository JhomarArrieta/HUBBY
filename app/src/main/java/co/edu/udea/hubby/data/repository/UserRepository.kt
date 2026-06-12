package co.edu.udea.hubby.data.repository

import co.edu.udea.hubby.data.model.Event
import co.edu.udea.hubby.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getCurrentUser(): Result<User> {
        return try {
            val uid = auth.currentUser!!.uid
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: User()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eventos creados por el usuario
    suspend fun getCreatedEvents(userId: String): Result<List<Event>> {
        return try {
            val snapshot = db.collection("events")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.copy(id = doc.id)
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Eventos en los que el usuario está inscrito
    suspend fun getRegisteredEvents(userId: String): Result<List<Event>> {
        return try {
            // Primero obtenemos los IDs de eventos donde está inscrito
            val registrations = db.collection("registrations")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val eventIds = registrations.documents.mapNotNull {
                it.getString("eventId")
            }

            if (eventIds.isEmpty()) return Result.success(emptyList())

            // Luego obtenemos los eventos
            val events = mutableListOf<Event>()
            eventIds.chunked(10).forEach { chunk ->
                val snapshot = db.collection("events")
                    .whereIn("__name__", chunk)
                    .get()
                    .await()
                events.addAll(snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                })
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar campus del usuario
    suspend fun updateCampus(campus: String): Result<Unit> {
        return try {
            val uid = auth.currentUser!!.uid
            db.collection("users").document(uid)
                .update("campus", campus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}