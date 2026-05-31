package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BreakTimeRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BreakScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BreakTimerViewModel
import com.example.ui.viewmodel.ViewModelFactory

sealed class Screen {
    object Login : Screen()
    data class Break(val username: String) : Screen()
    object Admin : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: BreakTimeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup state repository
        repository = BreakTimeRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Login -> {
                            val authViewModel: AuthViewModel = viewModel(
                                factory = ViewModelFactory(application, repository)
                            )
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = { username, role ->
                                    if (role == "admin") {
                                        currentScreen = Screen.Admin
                                    } else {
                                        currentScreen = Screen.Break(username)
                                    }
                                }
                            )
                        }
                        is Screen.Break -> {
                            val breakViewModel: BreakTimerViewModel = viewModel(
                                key = "break_timer_vm_${screen.username}",
                                factory = ViewModelFactory(application, repository, screen.username)
                            )
                            BreakScreen(
                                username = screen.username,
                                viewModel = breakViewModel,
                                onLogout = {
                                    // Status is system-controlled (completed / late)
                                    // Preserving status for the Admin campus board
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                        is Screen.Admin -> {
                            val adminViewModel: AdminViewModel = viewModel(
                                factory = ViewModelFactory(application, repository)
                            )
                            AdminDashboardScreen(
                                viewModel = adminViewModel,
                                onLogout = {
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.clearListeners()
    }
}
