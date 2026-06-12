package co.edu.udea.hubby.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import co.edu.udea.hubby.data.model.Event
import co.edu.udea.hubby.data.model.User
import co.edu.udea.hubby.data.repository.EventRepository
import co.edu.udea.hubby.data.repository.UserRepository
import co.edu.udea.hubby.utils.categories
import co.edu.udea.hubby.utils.getCategoryEmoji
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    onEventClick: (String) -> Unit
) {
    val eventRepository = remember { EventRepository() }
    val userRepository = remember { UserRepository() }

    var currentUser by remember { mutableStateOf<User?>(null) }
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var popularEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var displayedEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar usuario y eventos
    LaunchedEffect(Unit) {
        val userResult = userRepository.getCurrentUser()
        if (userResult.isSuccess) {
            currentUser = userResult.getOrNull()
            val campus = currentUser?.campus ?: "Ciudad Universitaria"

            val eventsResult = eventRepository.getEventsByCampus(campus)
            val popularResult = eventRepository.getPopularEvents(campus)

            if (eventsResult.isSuccess) {
                allEvents = eventsResult.getOrDefault(emptyList())
                displayedEvents = allEvents
            }
            if (popularResult.isSuccess) {
                popularEvents = popularResult.getOrDefault(emptyList())
            }
        }
        isLoading = false
    }

    // Filtrar por categoría o búsqueda
    LaunchedEffect(selectedCategory, searchQuery) {
        displayedEvents = allEvents.filter { event ->
            val matchesCategory = selectedCategory == null || event.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() ||
                    event.title.contains(searchQuery, ignoreCase = true) ||
                    event.place.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear evento", tint = Color.White)
            }
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
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Header
                item {
                    HomeHeader(
                        campus = currentUser?.campus ?: "Ciudad Universitaria",
                        onLogout = onLogout
                    )
                }

                // Barra de búsqueda
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar eventos...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }

                // Categorías
                item {
                    Text(
                        "Categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                }
                            )
                        }
                    }
                }

                // Eventos populares
                if (popularEvents.isNotEmpty()) {
                    item {
                        Text(
                            "Eventos Populares",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularEvents) { event ->
                                PopularEventCard(event = event, onEventClick = onEventClick)  // ← agrega esto
                            }
                        }
                    }
                }

                // Próximos eventos
                item {
                    Text(
                        "Próximos Eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                if (displayedEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay eventos disponibles", color = Color.Gray)
                        }
                    }
                } else {
                    items(displayedEvents) { event ->
                        EventListCard(
                            event = event,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onEventClick = onEventClick  // ← agrega esto
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(campus: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HUBBY",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onLogout) {
                    Text("Salir", color = Color.White)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("📍 $campus", color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Conectando intereses,\ncreando experiencias.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryChip(category: String, isSelected: Boolean, onClick: () -> Unit) {
    val emoji = getCategoryEmoji(category)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(72.dp).width(88.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                category,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White else Color.Unspecified
            )
        }
    }
}

@Composable
fun PopularEventCard(event: Event, onEventClick: (String) -> Unit) {
    val emoji = getCategoryEmoji(event.category)
    Card(
        onClick = { onEventClick(event.id) },
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Imagen o color por categoría
            if (event.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = MaterialTheme.typography.displaySmall)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(event.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("📍 ${event.place}", style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray, maxLines = 1)
                Text("👥 ${event.slotsLeft} cupos", style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }
        }
    }
}

@Composable
fun EventListCard(event: Event, modifier: Modifier = Modifier, onEventClick: (String) -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM • HH:mm", Locale("es", "CO"))
    val emoji = getCategoryEmoji(event.category)

    Card(
        onClick = { onEventClick(event.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Imagen o emoji
            if (event.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("📅 ${dateFormat.format(Date(event.date))}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("📍 ${event.place}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("👥 ${event.slotsLeft} cupos",
                        style = MaterialTheme.typography.bodySmall)
                    StatusChip(status = event.status)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "activo" -> MaterialTheme.colorScheme.primary
        "lleno" -> MaterialTheme.colorScheme.error
        "cancelado" -> MaterialTheme.colorScheme.outline
        "finalizado" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}