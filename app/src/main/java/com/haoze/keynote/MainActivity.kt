package com.haoze.keynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.haoze.keynote.ui.navigation.AppNavigation
import com.haoze.keynote.ui.theme.DarkModeManager
import com.haoze.keynote.ui.theme.KeyNoteTheme
import com.haoze.keynote.ui.theme.toDarkModePreference
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferencesManager = remember { PreferencesManager(applicationContext) }
            val darkModePref by preferencesManager.darkModePreference
                .map { it.toDarkModePreference() }
                .collectAsState(initial = com.haoze.keynote.ui.theme.DarkModePreference.SYSTEM)

            val darkModeManager = remember(darkModePref) {
                DarkModeManager(preference = darkModePref)
            }

            val isDarkMode = darkModeManager.isDarkMode()

            SideEffect {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDarkMode
            }

            KeyNoteTheme(darkModeManager = darkModeManager) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
