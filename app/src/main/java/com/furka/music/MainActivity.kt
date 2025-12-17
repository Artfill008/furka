package com.furka.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

import com.furka.music.data.SettingsDataStore
import com.furka.music.ui.screens.LibraryScreen
import com.furka.music.ui.screens.PlayerScreen
import com.furka.music.ui.screens.SetupScreen
import com.furka.music.ui.theme.FurkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FurkaTheme {
                val navController = rememberNavController()
                val settingsDataStore = SettingsDataStore(this)
                val settings by settingsDataStore.settingsFlow.collectAsState(
                    initial = com.furka.music.data.AppSettings()
                )

                val startDestination = if (settings.isSetupComplete) "library" else "setup"

                NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(route = "setup") {
                            SetupScreen(
                                onSetupComplete = {
                                    // Navigate with a soft physics slide into library.
                                    navController.navigate("library") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "library",
                            enterTransition = {
                                slideInVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    initialOffsetY = { -it / 3 }
                                ) + fadeIn()
                            },
                            exitTransition = {
                                slideOutVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    targetOffsetY = { -it / 3 }
                                ) + fadeOut()
                            }
                        ) {
                            val viewModel: com.furka.music.ui.viewmodel.LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                viewModel.loadLibrary()
                            }
                            LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { navController.navigate("player") }
                            )
                        }
                        
                        composable(
                            route = "player",
                            enterTransition = {
                                slideInVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    initialOffsetY = { it }
                                ) + fadeIn()
                            },
                            exitTransition = {
                                slideOutVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    targetOffsetY = { it }
                                ) + fadeOut()
                            }
                        ) {
                            PlayerScreen(
                                onDismiss = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

}