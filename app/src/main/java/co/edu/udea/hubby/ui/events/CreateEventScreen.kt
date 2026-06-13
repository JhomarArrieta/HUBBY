package co.edu.udea.hubby.ui.events


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.edu.udea.hubby.data.model.Event
import co.edu.udea.hubby.data.repository.EventRepository
import co.edu.udea.hubby.data.repository.UserRepository
import co.edu.udea.hubby.utils.categories
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import co.edu.udea.hubby.data.repository.StorageRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(onEventCreated: () -> Unit, onBack: () -> Unit) {
    val eventRepository = remember { EventRepository() }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

    // Date picker
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(year, month, day)
            selectedDateMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time picker
    val timePicker = TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            selectedDateMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val storageRepository = remember { StorageRepository() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del evento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text("Lugar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = slots,
                onValueChange = { slots = it.filter { c -> c.isDigit() } },
                label = { Text("Cupos disponibles") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Selector de categoría
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Selector de fecha y hora
            Text("Fecha y hora del evento", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { datePicker.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(selectedDateMillis)))
                }
                OutlinedButton(
                    onClick = { timePicker.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(selectedDateMillis)))
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            // Foto del evento (opcional)
            Text("Foto del evento (opcional)", style = MaterialTheme.typography.labelMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", style = MaterialTheme.typography.headlineMedium)
                        Text("Toca para agregar foto",
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            Button(
                onClick = {
                    // Validaciones
                    when {
                        title.isBlank() -> errorMessage = "El título es obligatorio"
                        description.isBlank() -> errorMessage = "La descripción es obligatoria"
                        place.isBlank() -> errorMessage = "El lugar es obligatorio"
                        slots.isBlank() -> errorMessage = "Los cupos son obligatorios"
                        selectedDateMillis < System.currentTimeMillis() ->
                            errorMessage = "La fecha debe ser futura"
                        else -> {
                            scope.launch {
                                isLoading = true
                                val userResult = userRepository.getCurrentUser()
                                val user = userResult.getOrNull()

                                val slotsInt = slots.toIntOrNull() ?: 0
                                val event = Event(
                                    title = title,
                                    description = description,
                                    place = place,
                                    campus = user?.campus ?: "Ciudad Universitaria",
                                    date = selectedDateMillis,
                                    category = selectedCategory,
                                    slots = slotsInt,
                                    slotsLeft = slotsInt,
                                    status = "activo",
                                    creatorId = user?.uid ?: "",
                                    creatorName = user?.name ?: ""
                                )
                                val result = eventRepository.createEvent(event)

                                if (result.isSuccess && selectedImageUri != null) {
                                    val eventId = result.getOrNull()!!
                                    val photoResult = storageRepository.uploadEventPhoto(selectedImageUri!!, eventId)
                                    if (photoResult.isSuccess) {
                                        eventRepository.updateEvent(
                                            eventId,
                                            mapOf("imageUrl" to photoResult.getOrNull()!!)
                                        )
                                    }
                                }

                                isLoading = false
                                if (result.isSuccess) onEventCreated()
                                else errorMessage = "Error al crear el evento, intenta de nuevo"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Crear Evento")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}