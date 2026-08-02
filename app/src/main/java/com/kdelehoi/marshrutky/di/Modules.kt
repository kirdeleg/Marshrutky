package com.kdelehoi.marshrutky.di

import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { ScheduleRepository(androidContext()) }
    single { PreferencesRepository(androidContext()) }
    single { ScheduleViewModel(get(), get()) }
    single { SettingsViewModel(get()) }
}
