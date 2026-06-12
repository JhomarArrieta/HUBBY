package co.edu.udea.hubby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import co.edu.udea.hubby.data.repository.AuthRepository
import co.edu.udea.hubby.ui.auth.LoginScreen
import co.edu.udea.hubby.ui.auth.RegisterScreen
import co.edu.udea.hubby.ui.events.CreateEventScreen
import co.edu.udea.hubby.ui.events.EventDetailScreen
import co.edu.udea.hubby.ui.events.EventsScreen
import co.edu.udea.hubby.ui.theme.HUBBYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository()

        setContent {
            HUBBYTheme {
                var currentScreen by remember {
                    mutableStateOf(
                        if (authRepository.isUserLoggedIn()) "home" else "login"
                    )
                }
                var selectedEventId by remember { mutableStateOf("") }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { currentScreen = "home" },
                        onGoToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        onRegisterSuccess = { currentScreen = "home" },
                        onGoToLogin = { currentScreen = "login" }
                    )
                    "home" -> EventsScreen(
                        onLogout = {
                            authRepository.logout()
                            currentScreen = "login"
                        },
                        onCreateEvent = { currentScreen = "createEvent" },
                        onEventClick = { id ->
                            selectedEventId = id
                            currentScreen = "eventDetail"
                        }
                    )
                    "createEvent" -> CreateEventScreen(
                        onEventCreated = { currentScreen = "home" },
                        onBack = { currentScreen = "home" }
                    )
                    "eventDetail" -> EventDetailScreen(
                        eventId = selectedEventId,
                        onBack = { currentScreen = "home" },
                        onEditEvent = { id ->
                            selectedEventId = id
                            currentScreen = "editEvent"
                        }
                    )
                }
            }
        }
    }
}