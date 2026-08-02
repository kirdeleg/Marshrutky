package com.kdelehoi.marshrutky.di

import com.kdelehoi.marshrutky.data.local.RoutesCache
import com.kdelehoi.marshrutky.data.remote.RoutesRemoteDataSource
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single { RoutesCache(androidContext(), get()) }
    single { RoutesRemoteDataSource(get()) }
    single { ScheduleRepository(androidContext(), get(), get(), get()) }
    single { PreferencesRepository(androidContext()) }
    single { ScheduleViewModel(get(), get()) }
    single { SettingsViewModel(get()) }
}
