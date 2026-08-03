package com.kdelehoi.marshrutky.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.kdelehoi.marshrutky.R
import kotlinx.serialization.Serializable

sealed class Screen : NavKey {
    /** Три головні вкладки живуть в одному пейджері, тож у стеку навігації це один пункт. */
    @Serializable
    data object Main : Screen()

    @Serializable
    data class RouteDetail(val routeId: String) : Screen()
}

enum class TopLevelDestination(
    val icon: ImageVector,
    @param:StringRes val labelRes: Int
) {
    FAVORITES(Icons.Default.Star, R.string.tab_favorites),
    NEAREST(Icons.Default.Schedule, R.string.tab_nearest),
    ROUTES(Icons.Default.DirectionsBus, R.string.tab_routes),
    SETTINGS(Icons.Default.Settings, R.string.tab_settings)
}
