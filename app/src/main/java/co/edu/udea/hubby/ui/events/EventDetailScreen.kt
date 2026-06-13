package co.edu.udea.hubby.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.hubby.data.repository.EventRepository
import co.edu.udea.hubby.data.repository.UserRepository
import co.edu.udea.hubby.utils.getCategoryEmoji
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEditEvent: (String) -> Unit
) {
    val eventRepository = remember { EventRepository() }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var event by remember { mutableStateOf<co.edu.udea.hubby.data.model.Event?>(null) }
    var isRegistered by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    var hasRated by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(0) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Cargar evento y estado de inscripción
    LaunchedEffect(eventId) {
        val eventResult = eventRepository.getEventById(eventId)
        if (eventResult.isSuccess) {
            event = eventResult.getOrNull()
            isRegistered = eventRepository.isUserRegistered(eventId, currentUserId)
            if (isRegistered) {
                hasRated = eventRepository.hasUserRated(eventId, currentUserId)
            }
        }
        isLoading = false
    }

    // Diálogo de confirmación para cancelar
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar evento") },
            text = { Text("¿Estás seguro de que quieres cancelar este evento?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    scope.launch {
                        actionLoading = true
                        val result = eventRepository.cancelEvent(eventId)
                        if (result.isSuccess) {
                            event = event?.copy(status = "cancelado")
                            message = "Evento cancelado"
                        } else message = "Error al cancelar"
                        actionLoading = false
                    }
                }) { Text("Sí, cancelar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("No") }
            }
        )
    }

    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Calificar evento") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿Cómo estuvo el evento?",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { selectedRating = star }) {
                                Text(
                                    if (star <= selectedRating) "⭐" else "☆",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                    if (selectedRating > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (selectedRating) {
                                1 -> "Muy malo 😞"
                                2 -> "Regular 😐"
                                3 -> "Bien 🙂"
                                4 -> "Muy bien 😊"
                                5 -> "Excelente! 🤩"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedRating > 0) {
                            showRatingDialog = false
                            scope.launch {
                                actionLoading = true
                                val result = eventRepository.rateEvent(
                                    eventId, currentUserId, selectedRating
                                )
                                if (result.isSuccess) {
                                    hasRated = true
                                    event = eventRepository.getEventById(eventId).getOrNull()
                                    message = "¡Gracias por tu calificación!"
                                } else {
                                    message = "Error al calificar"
                                }
                                actionLoading = false
                            }
                        }
                    },
                    enabled = selectedRating > 0
                ) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Evento no encontrado")
            }
        } else {
            val e = event!!
            val dateFormat = SimpleDateFormat("EEEE dd 'de' MMMM yyyy • HH:mm", Locale("es", "CO"))
            val emoji = getCategoryEmoji(e.category)
            val isCreator = e.creatorId == currentUserId

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Imagen / banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = MaterialTheme.typography.displayLarge)
                }

                Column(modifier = Modifier.padding(16.dp)) {

                    // Título y estado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            e.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusChip(status = e.status)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Info del evento
                    InfoRow(emoji = "📅", text = dateFormat.format(Date(e.date)))
                    Spacer(Modifier.height(8.dp))
                    InfoRow(emoji = "📍", text = "${e.place} • ${e.campus}")
                    Spacer(Modifier.height(8.dp))
                    InfoRow(emoji = "🏷️", text = e.category)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(emoji = "👥", text = "${e.slotsLeft} cupos disponibles de ${e.slots}")
                    Spacer(Modifier.height(8.dp))
                    InfoRow(emoji = "👤", text = "Creado por ${e.creatorName}")

                    if (e.rating > 0) {
                        Spacer(Modifier.height(8.dp))
                        InfoRow(emoji = "⭐", text = "${"%.1f".format(e.rating)} (${e.ratingCount} calificaciones)")
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    // Descripción
                    Text("Descripción", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(e.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(24.dp))

                    // Mensaje de feedback
                    if (message.isNotEmpty()) {
                        Text(message, color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }

                    // Botones según rol
                    if (isCreator) {
                        // Es el creador
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onEditEvent(eventId) },
                                modifier = Modifier.weight(1f),
                                enabled = e.status == "activo"
                            ) {
                                Text("✏️ Editar")
                            }
                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = e.status == "activo" && !actionLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("❌ Cancelar")
                            }
                        }
                    } else {
                        // Es otro usuario
                        Button(
                            onClick = {
                                scope.launch {
                                    actionLoading = true
                                    val result = eventRepository.joinEvent(eventId, currentUserId)
                                    if (result.isSuccess) {
                                        isRegistered = true
                                        event = eventRepository.getEventById(eventId).getOrNull()
                                        message = "¡Te uniste al evento!"
                                    } else {
                                        message = "Error al unirte, intenta de nuevo"
                                    }
                                    actionLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isRegistered &&
                                    e.status == "activo" &&
                                    !actionLoading
                        ) {
                            if (actionLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(
                                    when {
                                        isRegistered -> "✅ Ya estás inscrito"
                                        e.status == "lleno" -> "😔 Evento lleno"
                                        e.status == "cancelado" -> "❌ Evento cancelado"
                                        else -> "🙌 Unirme"
                                    }
                                )
                            }

                        }
                        if (isRegistered && e.status == "finalizado") {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showRatingDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !hasRated,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (hasRated) "✅ Ya calificaste este evento" else "⭐ Calificar evento")
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun InfoRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}