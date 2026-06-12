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
import co.edu.udea.hubby.data.repository.EventRepository
import co.edu.udea.hubby.utils.categories
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    eventId: String,
    onEventUpdated: () -> Unit,
    onBack: () -> Unit
) {
    val eventRepository = remember { EventRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Cargar datos del evento existente
    LaunchedEffect(eventId) {
        val result = eventRepository.getEventById(eventId)
        if (result.isSuccess) {
            val event = result.getOrNull()!!
            title = event.title
            description = event.description
            place = event.place
            slots = event.slots.toString()
            selectedCategory = event.category
            selectedDateMillis = event.date
        }
        isLoading = false
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                    label = { Text("Cupos totales") },
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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

                Button(
                    onClick = {
                        when {
                            title.isBlank() -> errorMessage = "El título es obligatorio"
                            description.isBlank() -> errorMessage = "La descripción es obligatoria"
                            place.isBlank() -> errorMessage = "El lugar es obligatorio"
                            slots.isBlank() -> errorMessage = "Los cupos son obligatorios"
                            else -> {
                                scope.launch {
                                    actionLoading = true
                                    val updatedFields = mapOf(
                                        "title" to title,
                                        "description" to description,
                                        "place" to place,
                                        "slots" to (slots.toIntOrNull() ?: 0),
                                        "category" to selectedCategory,
                                        "date" to selectedDateMillis
                                    )
                                    val result = eventRepository.updateEvent(eventId, updatedFields)
                                    actionLoading = false
                                    if (result.isSuccess) onEventUpdated()
                                    else errorMessage = "Error al actualizar, intenta de nuevo"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionLoading
                ) {
                    if (actionLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Guardar cambios")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}