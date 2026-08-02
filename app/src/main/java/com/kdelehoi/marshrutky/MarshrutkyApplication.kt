package com.kdelehoi.marshrutky

import android.app.Application
import com.kdelehoi.marshrutky.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MarshrutkyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MarshrutkyApplication)
            modules(appModule)
        }
    }
}
