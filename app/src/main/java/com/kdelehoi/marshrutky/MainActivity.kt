package com.kdelehoi.marshrutky

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kdelehoi.marshrutky.ui.navigation.MarshrutkyNavGraph
import com.kdelehoi.marshrutky.ui.theme.MarshrutkyTheme
import com.kdelehoi.marshrutky.ui.theme.isDark
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

            // enableEdgeToEdge сам вирішує, світлі чи темні робити іконки системних смуг, і
            // питає про це системну тему. Наш власний вибір теми його не обходить, тому смуги
            // треба переоголошувати щоразу, коли тема застосунку змінилася.
            val darkTheme = themeMode.isDark()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme }
                )
                onDispose {}
            }

            MarshrutkyTheme(themeMode = themeMode) {
                MarshrutkyNavGraph(
                    scheduleViewModel = scheduleViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    private companion object {
        const val TRANSPARENT = Color.TRANSPARENT

        // Ті самі значення, що enableEdgeToEdge бере за замовчуванням для нижньої смуги: до
        // Android 10 прозорою вона бути не вміє, тож потрібна напівпрозора підкладка.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
