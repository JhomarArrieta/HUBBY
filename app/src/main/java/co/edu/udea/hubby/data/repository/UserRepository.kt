package co.edu.udea.hubby.data.repository

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
}