package co.edu.udea.hubby.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.udea.hubby.data.model.Event
import co.edu.udea.hubby.data.model.User
import co.edu.udea.hubby.data.repository.UserRepository
import co.edu.udea.hubby.ui.events.StatusChip
import co.edu.udea.hubby.utils.getCategoryEmoji
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit
) {
    val userRepository = remember { UserRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var user by remember { mutableStateOf<User?>(null) }
    var createdEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var registeredEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCampusDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val campusOptions = listOf("Ciudad Universitaria", "Seccional Oriente", "Seccional Urabá")

    LaunchedEffect(Unit) {
        val userResult = userRepository.getCurrentUser()
        if (userResult.isSuccess) {
            user = userResult.getOrNull()
            val uid = user?.uid ?: currentUserId
            val createdResult = userRepository.getCreatedEvents(uid)
            val registeredResult = userRepository.getRegisteredEvents(uid)
            if (createdResult.isSuccess) createdEvents = createdResult.getOrDefault(emptyList())
            if (registeredResult.isSuccess) registeredEvents = registeredResult.getOrDefault(emptyList())
        }
        isLoading = false
    }

    // Diálogo para cambiar campus
    if (showCampusDialog) {
        AlertDialog(
            onDismissRequest = { showCampusDialog = false },
            title = { Text("Selecciona tu campus") },
            text = {
                Column {
                    campusOptions.forEach { campus ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = user?.campus == campus,
                                onClick = {
                                    user = user?.copy(campus = campus)
                                }
                            )
                            Text(campus)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCampusDialog = false
                    val campus = user?.campus ?: return@TextButton
                    scope.launch {
                        userRepository.updateCampus(campus)
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showCampusDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header del perfil
                item {
                    ProfileHeader(
                        user = user,
                        onChangeCampus = { showCampusDialog = true }
                    )
                }

                // Tabs
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Creados (${createdEvents.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Inscritos (${registeredEvents.size})") }
                        )
                    }
                }

                // Lista según tab seleccionado
                val eventsList = if (selectedTab == 0) createdEvents else registeredEvents

                if (eventsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (selectedTab == 0) "No has creado eventos aún"
                                else "No estás inscrito en ningún evento",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    items(eventsList) { event ->
                        ProfileEventCard(
                            event = event,
                            onClick = { onEventClick(event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User?, onChangeCampus: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {

            // Avatar con inicial
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(user?.name ?: "Usuario", style = MaterialTheme.typography.titleLarge,
                color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(user?.career ?: "", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))

            // Campus clickeable
            OutlinedButton(
                onClick = onChangeCampus,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Text("📍 ${user?.campus ?: "Ciudad Universitaria"}")
            }
        }
    }
}

@Composable
fun ProfileEventCard(event: Event, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM • HH:mm", Locale("es", "CO"))
    val emoji = getCategoryEmoji(event.category)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold, maxLines = 1)
                Text("📅 ${dateFormat.format(Date(event.date))}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("📍 ${event.place}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status = event.status)
        }
    }
}