package co.edu.udea.hubby.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import co.edu.udea.hubby.data.repository.StorageRepository
import co.edu.udea.hubby.data.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onProfileUpdated: () -> Unit,
    onBack: () -> Unit
) {
    val userRepository = remember { UserRepository() }
    val storageRepository = remember { StorageRepository() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var career by remember { mutableStateOf("") }
    var currentPhotoUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Launcher para seleccionar imagen de la galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Cargar datos actuales del usuario
    LaunchedEffect(Unit) {
        val result = userRepository.getCurrentUser()
        if (result.isSuccess) {
            val user = result.getOrNull()!!
            name = user.name
            career = user.career
            currentPhotoUrl = user.photoUrl
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Foto de perfil
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                    } else if (currentPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = currentPhotoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botón de editar foto
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }) {
                            Text("📷", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text(
                    "Toca la foto para cambiarla",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = career,
                    onValueChange = { career = it },
                    label = { Text("Carrera") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        when {
                            name.isBlank() -> errorMessage = "El nombre es obligatorio"
                            career.isBlank() -> errorMessage = "La carrera es obligatoria"
                            else -> {
                                scope.launch {
                                    isSaving = true
                                    errorMessage = ""

                                    // Subir foto si se seleccionó una nueva
                                    if (selectedImageUri != null) {
                                        val photoResult = storageRepository
                                            .uploadProfilePhoto(selectedImageUri!!)
                                        if (photoResult.isSuccess) {
                                            userRepository.updateProfilePhoto(
                                                photoResult.getOrNull()!!
                                            )
                                        }
                                    }

                                    // Actualizar nombre y carrera
                                    val result = userRepository.updateProfile(name, career)
                                    isSaving = false

                                    if (result.isSuccess) onProfileUpdated()
                                    else errorMessage = "Error al guardar, intenta de nuevo"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Guardar cambios")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}