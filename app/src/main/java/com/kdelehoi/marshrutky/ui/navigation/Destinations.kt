package com.kdelehoi.marshrutky.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen : NavKey {
    @Serializable
    data object Home : Screen()

    @Serializable
    data object FullSchedule : Screen()
}
