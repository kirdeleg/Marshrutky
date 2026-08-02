package com.kdelehoi.marshrutky.data.repository

import android.content.Context
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.ScheduleFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ScheduleRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadRoutes(): List<Route> = withContext(Dispatchers.IO) {
        val raw = context.assets.open(SCHEDULE_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<ScheduleFile>(raw).routes
    }

    companion object {
        private const val SCHEDULE_ASSET = "schedule.json"
    }
}
