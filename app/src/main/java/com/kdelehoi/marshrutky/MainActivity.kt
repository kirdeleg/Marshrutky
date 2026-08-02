package com.kdelehoi.marshrutky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kdelehoi.marshrutky.ui.navigation.MarshrutkyNavGraph
import com.kdelehoi.marshrutky.ui.theme.MarshrutkyTheme
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val scheduleViewModel: ScheduleViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()

            MarshrutkyTheme(themeMode = themeMode) {
                MarshrutkyNavGraph(
                    scheduleViewModel = scheduleViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
