package com.kdelehoi.marshrutky.di

import com.kdelehoi.marshrutky.data.local.RoutesCache
import com.kdelehoi.marshrutky.data.remote.NetworkMonitor
import com.kdelehoi.marshrutky.data.remote.RoutesRemoteDataSource
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.data.repository.createSettingsDataStore
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single { RoutesCache(androidContext(), get()) }
    single { RoutesRemoteDataSource(get()) }
    single { NetworkMonitor(androidContext()) }
    // Репозиторії живуть скільки процес — саме вони тримають дані між екранами.
    single { ScheduleRepository(get(), get(), get()) }
    single { createSettingsDataStore(androidContext()) }
    single { PreferencesRepository(get()) }
    // ViewModel живе рівно скільки екран: інакше viewModelScope не скасовується ніколи, і все,
    // що в ньому запущено, працює у фоні без жодного глядача.
    viewModel { ScheduleViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
