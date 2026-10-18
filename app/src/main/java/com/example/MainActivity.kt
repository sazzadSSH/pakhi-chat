package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.FirestorePakhiRepository
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.PakhiTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel

sealed interface Screen {
    data object Home : Screen
    data class Chat(
        val chatId: String,
        val otherUserId: String,
        val otherName: String,
        val otherPakhiId: String
    ) : Screen
    data object Profile : Screen
    data object Settings : Screen
    data object Admin : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var firestoreRepository: FirestorePakhiRepository
    private lateinit var authViewModel: AuthViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var adminViewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firestoreRepository = FirestorePakhiRepository(this, lifecycleScope)

        authViewModel = AuthViewModel(firestoreRepository)
        chatViewModel = ChatViewModel(firestoreRepository)
        adminViewModel = AdminViewModel(firestoreRepository)

        setContent {
            PakhiTheme {
                MainContent(
                    authViewModel = authViewModel,
                    chatViewModel = chatViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }
}

@Composable
fun MainContent(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    adminViewModel: AdminViewModel
) {
    val firebaseUser by authViewModel.firebaseUser.collectAsState()
    val appConfig by authViewModel.appConfig.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // If user is not authenticated with Google, show SignInScreen (unless in Admin mode)
    if (firebaseUser == null && currentScreen !is Screen.Admin) {
        SignInScreen(
            onAuthSuccess = { currentScreen = Screen.Home },
            onNavigateToAdmin = { currentScreen = Screen.Admin }
        )
        return
    }

    // If maintenance mode is enabled by Admin
    if (appConfig?.maintenanceEnabled == true && currentScreen !is Screen.Admin) {
        MaintenanceScreen(
            message = appConfig?.maintenanceMessage ?: "পাখির সার্ভার রক্ষণাবেক্ষণ চলছে।",
            onNavigateToAdmin = { currentScreen = Screen.Admin }
        )
        return
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                slideOutHorizontally { width -> -width } + fadeOut()
            )
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    authViewModel = authViewModel,
                    chatViewModel = chatViewModel,
                    onOpenChat = { chatId, otherUserId, otherName, otherPakhiId ->
                        currentScreen = Screen.Chat(chatId, otherUserId, otherName, otherPakhiId)
                    },
                    onNavigateToProfile = { currentScreen = Screen.Profile },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToAdmin = { currentScreen = Screen.Admin }
                )
            }

            is Screen.Chat -> {
                ChatScreen(
                    chatId = screen.chatId,
                    otherUserId = screen.otherUserId,
                    otherName = screen.otherName,
                    otherPakhiId = screen.otherPakhiId,
                    authViewModel = authViewModel,
                    chatViewModel = chatViewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }

            is Screen.Profile -> {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { currentScreen = Screen.Home },
                    onNavigateToAdmin = { currentScreen = Screen.Admin },
                    onSignOut = { currentScreen = Screen.Home }
                )
            }

            is Screen.Admin -> {
                AdminScreen(
                    adminViewModel = adminViewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
