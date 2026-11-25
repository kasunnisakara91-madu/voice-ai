package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AssistantVoiceTheme
import com.example.viewmodel.AssistantViewModel

private const val ROUTE_ASSISTANT = "assistant"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any Compose UI or ViewModel runs.
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val viewModel: AssistantViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            AssistantVoiceTheme(darkTheme = uiState.settings.darkTheme) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_ASSISTANT
                ) {
                    composable(ROUTE_ASSISTANT) {
                        AssistantScreen(
                            uiState = uiState,
                            onStartListening = viewModel::startVoiceListening,
                            onStopListening = viewModel::stopVoiceListening,
                            onStopSpeaking = viewModel::stopSpeaking,
                            onMicPermissionResult = viewModel::onMicPermissionResult,
                            onSendMessage = { text -> viewModel.sendMessage(text, isVoice = false) },
                            onSpeakMessage = viewModel::speakMessage,
                            onToggleVoiceLanguage = viewModel::toggleSpeechLocaleQuick,
                            onToggleJarvisStandby = viewModel::setJarvisStandbyEnabled,
                            onToggleJarvisAutoListen = viewModel::setJarvisAutoListen,
                            onDismissError = viewModel::dismissError,
                            onOpenSettings = {
                                viewModel.refreshDiagnostics()
                                navController.navigate(ROUTE_SETTINGS)
                            }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            onBack = { navController.popBackStack() },
                            onSelectModel = viewModel::setSelectedModel,
                            onTestGeminiConnection = viewModel::testGeminiConnection,
                            onSelectLanguage = viewModel::setPreferredLanguage,
                            onSelectSpeechLocale = viewModel::setSpeechInputLocale,
                            onSelectVoiceName = viewModel::setSelectedVoiceName,
                            onSpeechSpeedChange = viewModel::setSpeechSpeed,
                            onSpeechPitchChange = viewModel::setSpeechPitch,
                            onAutoSpeakChange = viewModel::setAutoSpeak,
                            onDarkThemeChange = viewModel::setDarkTheme,
                            onSaveHistoryChange = viewModel::setSaveHistory,
                            onIncludeContextChange = viewModel::setIncludeDeviceContext,
                            onClearHistory = viewModel::clearConversationHistory,
                            onMicPermissionResult = viewModel::onMicPermissionResult,
                            onInstallTtsData = {
                                try {
                                    startActivity(viewModel.createInstallTtsDataIntent())
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}
