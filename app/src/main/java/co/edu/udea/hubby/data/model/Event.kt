package co.edu.udea.hubby.data.model

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val place: String = "",
    val campus: String = "",
    val date: Long = 0L,
    val category: String = "",
    val slots: Int = 0,
    val slotsLeft: Int = 0,
    val status: String = "activo",
    val creatorId: String = "",
    val creatorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val registeredCount: Int = 0,
    val popularityScore: Float = 0f,
    val imageUrl: String = ""
)