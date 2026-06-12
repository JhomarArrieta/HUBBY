package co.edu.udea.hubby.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val career: String = "",
    val campus: String = "Ciudad Universitaria",
    val createdAt: Long = System.currentTimeMillis()
)