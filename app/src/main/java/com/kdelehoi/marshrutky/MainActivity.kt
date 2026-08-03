package com.kdelehoi.marshrutky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kdelehoi.marshrutky.ui.navigation.MarshrutkyNavGraph
import com.kdelehoi.marshrutky.ui.theme.MarshrutkyTheme
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    // Тепер це справжні ViewModel, прив'язані до ViewModelStore активності, а не вічні
    // singleton'и з контейнера Koin: коли активність завершується, viewModelScope скасовується.
    private val scheduleViewModel: ScheduleViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Саме collectAsStateWithLifecycle, а не collectAsState: звичайний збирач працює,
            // поки жива композиція, а вона переживає згортання застосунку. Без цього годинник
            // і читачі DataStore крутилися б у фоні, хоч на них ніхто не дивиться.
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            MarshrutkyTheme(themeMode = themeMode) {
                MarshrutkyNavGraph(
                    scheduleViewModel = scheduleViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
