package co.edu.udea.hubby.data.repository

import co.edu.udea.hubby.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    // Obtener eventos por campus ordenados por fecha
    suspend fun getEventsByCampus(campus: String): Result<List<Event>> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("campus", campus)
                .whereEqualTo("status", "activo")
                .orderBy("date", Query.Direction.ASCENDING)
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

    // Obtener top 3 eventos populares
    suspend fun getPopularEvents(campus: String): Result<List<Event>> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("campus", campus)
                .whereEqualTo("status", "activo")
                .orderBy("popularityScore", Query.Direction.DESCENDING)
                .limit(3)
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

    // Obtener eventos por categoría
    suspend fun getEventsByCategory(campus: String, category: String): Result<List<Event>> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("campus", campus)
                .whereEqualTo("category", category)
                .whereEqualTo("status", "activo")
                .orderBy("date", Query.Direction.ASCENDING)
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

    suspend fun createEvent(event: Event): Result<String> {
        return try {
            val docRef = eventsCollection.add(event).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verificar si el usuario ya está inscrito
    suspend fun isUserRegistered(eventId: String, userId: String): Boolean {
        return try {
            val snapshot = db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // Unirse a un evento
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = eventsCollection.document(eventId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val slotsLeft = snapshot.getLong("slotsLeft")?.toInt() ?: 0

                if (slotsLeft <= 0) throw Exception("No hay cupos disponibles")

                // Crear registro
                val registration = hashMapOf(
                    "eventId" to eventId,
                    "userId" to userId,
                    "registeredAt" to System.currentTimeMillis(),
                    "rated" to false,
                    "ratingGiven" to 0
                )
                val registrationRef = db.collection("registrations").document()
                transaction.set(registrationRef, registration)

                // Actualizar cupos y popularidad
                val newSlotsLeft = slotsLeft - 1
                val registeredCount = (snapshot.getLong("registeredCount")?.toInt() ?: 0) + 1
                val rating = snapshot.getDouble("rating")?.toFloat() ?: 0f
                val popularityScore = (rating * 2) + registeredCount

                transaction.update(eventRef, mapOf(
                    "slotsLeft" to newSlotsLeft,
                    "registeredCount" to registeredCount,
                    "popularityScore" to popularityScore,
                    "status" to if (newSlotsLeft <= 0) "lleno" else "activo"
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener evento por ID
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val doc = eventsCollection.document(eventId).get().await()
            val event = doc.toObject(Event::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Evento no encontrado"))
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cancelar evento
    suspend fun cancelEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId)
                .update("status", "cancelado")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(eventId: String, updatedFields: Map<String, Any>): Result<Unit> {
        return try {
            eventsCollection.document(eventId)
                .update(updatedFields)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Marcar eventos pasados como finalizados automáticamente
    suspend fun updateExpiredEvents(campus: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val snapshot = eventsCollection
                .whereEqualTo("campus", campus)
                .whereEqualTo("status", "activo")
                .whereLessThan("date", now)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.update("status", "finalizado")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rateEvent(eventId: String, userId: String, rating: Int): Result<Unit> {
        return try {
            val eventRef = eventsCollection.document(eventId)

            // Actualizar rating del evento
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val currentRating = snapshot.getDouble("rating")?.toFloat() ?: 0f
                val ratingCount = snapshot.getLong("ratingCount")?.toInt() ?: 0
                val newRatingCount = ratingCount + 1
                val newRating = ((currentRating * ratingCount) + rating) / newRatingCount
                val registeredCount = snapshot.getLong("registeredCount")?.toInt() ?: 0
                val newPopularityScore = (newRating * 2) + registeredCount

                transaction.update(eventRef, mapOf(
                    "rating" to newRating,
                    "ratingCount" to newRatingCount,
                    "popularityScore" to newPopularityScore
                ))
            }.await()

            // Actualizar el registro del usuario por separado (fuera de la transacción)
            val regSnapshot = db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            regSnapshot.documents.firstOrNull()?.reference?.update(
                mapOf("rated" to true, "ratingGiven" to rating)
            )?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("RATE_ERROR", "Error al calificar: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Verificar si el usuario ya calificó
    suspend fun hasUserRated(eventId: String, userId: String): Boolean {
        return try {
            val snapshot = db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("rated", true)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

}

