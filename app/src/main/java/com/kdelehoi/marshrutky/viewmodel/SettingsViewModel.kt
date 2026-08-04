package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.domain.model.AppLanguage
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Те, від чого залежить вигляд усього застосунку, а не якогось одного екрана. */
data class AppearanceState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    /** Одним потоком, а не двома: інакше тему й мову читали б два окремі читачі того самого файлу. */
    val appearance: StateFlow<AppearanceState> = preferencesRepository.preferences
        .map { AppearanceState(themeMode = it.themeMode, language = it.language) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            // Як і решта стану: поки екрана не видно, читача DataStore тримати нема навіщо.
            started = SharingStarted.WhileSubscribed(STATE_UNSUBSCRIBE_DELAY_MILLIS),
            initialValue = AppearanceState()
        )

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.saveThemeMode(themeMode)
        }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.saveLanguage(language)
        }
    }
}
