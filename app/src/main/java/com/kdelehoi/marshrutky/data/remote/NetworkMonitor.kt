package com.kdelehoi.marshrutky.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

/**
 * Чи є взагалі сенс іти по мережу. Без цієї перевірки кожен запуск без зв'язку — це спроба
 * з'єднання і чекання таймауту, тобто марно розбуджений радіомодуль.
 */
class NetworkMonitor(private val context: Context) {

    val isOnline: Boolean
        get() {
            val manager = context.getSystemService<ConnectivityManager>() ?: return false
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
}
