package co.edu.udea.hubby.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Subir foto de perfil
    suspend fun uploadProfilePhoto(imageUri: Uri): Result<String> {
        return try {
            val uid = auth.currentUser!!.uid
            val ref = storage.reference.child("profiles/$uid.jpg")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Subir foto de evento
    suspend fun uploadEventPhoto(imageUri: Uri, eventId: String): Result<String> {
        return try {
            val ref = storage.reference.child("events/$eventId.jpg")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}