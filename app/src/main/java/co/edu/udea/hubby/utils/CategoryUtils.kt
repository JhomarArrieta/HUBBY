package co.edu.udea.hubby.utils

import co.edu.udea.hubby.R

val categories = listOf(
    "Deportes",
    "Música",
    "Estudio",
    "Arte y Cultura",
    "Entretenimiento",
    "Parche"
)

fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Deportes" -> "⚽"
        "Música" -> "🎵"
        "Estudio" -> "📚"
        "Arte y Cultura" -> "🎨"
        "Entretenimiento" -> "🎮"
        "Parche" -> "🤝"
        else -> "📌"
    }
}