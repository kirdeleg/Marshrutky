package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM
    )

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.saveThemeMode(themeMode)
        }
    }
}
