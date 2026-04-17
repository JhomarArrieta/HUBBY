package co.edu.udea.hubby.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import co.edu.udea.hubby.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Validar que el correo sea institucional
    fun isValidUdeaEmail(email: String): Boolean {
        return email.endsWith("@udea.edu.co")
    }

    // Registrar usuario nuevo
    suspend fun register(email: String, password: String, name: String, career: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user!!.uid

            val user = User(uid = uid, name = name, email = email, career = career)
            db.collection("users").document(uid).set(user).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Iniciar sesión
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verificar si ya hay sesión activa
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Cerrar sesión
    fun logout() = auth.signOut()
}