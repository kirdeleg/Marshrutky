package com.kdelehoi.marshrutky.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

/**
 * Чи є взагалі сенс іти по мережу. Без цієї перевірки кожен запуск без зв'язку — це спроба
 * з'єднання і чекання таймауту, тобто марно розбуджений радіомодуль.
 */
interface NetworkMonitor {
    val isOnline: Boolean
}

/** Питає систему. Окремо від інтерфейсу, щоб у тестах не тягнути за собою Context. */
class ConnectivityNetworkMonitor(private val context: Context) : NetworkMonitor {

    override val isOnline: Boolean
        get() {
            val manager = context.getSystemService<ConnectivityManager>() ?: return false
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
}
