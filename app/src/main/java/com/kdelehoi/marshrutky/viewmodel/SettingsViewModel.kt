package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.preferences
        .map { it.themeMode }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            // Як і решта стану: поки екрана не видно, читача DataStore тримати нема навіщо.
            started = SharingStarted.WhileSubscribed(UNSUBSCRIBE_DELAY_MILLIS),
            initialValue = ThemeMode.SYSTEM
        )

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.saveThemeMode(themeMode)
        }
    }

    private companion object {
        const val UNSUBSCRIBE_DELAY_MILLIS = 5_000L
    }
}
