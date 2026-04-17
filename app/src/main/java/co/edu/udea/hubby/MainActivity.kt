package co.edu.udea.hubby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import co.edu.udea.hubby.data.repository.AuthRepository
import co.edu.udea.hubby.ui.auth.LoginScreen
import co.edu.udea.hubby.ui.auth.RegisterScreen
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

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { currentScreen = "home" },
                        onGoToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        onRegisterSuccess = { currentScreen = "home" },
                        onGoToLogin = { currentScreen = "login" }
                    )
                    "home" -> {
                        // Aquí irá la pantalla principal en la Fase 3
                    }
                }
            }
        }
    }
}